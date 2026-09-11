package com.rolecompass.service;

import com.rolecompass.aggregation.FeatureAggregationService;
import com.rolecompass.dto.*;
import com.rolecompass.entity.Answer;
import com.rolecompass.entity.AnswerId;
import com.rolecompass.entity.Question;
import com.rolecompass.entity.Session;
import com.rolecompass.entity.User;
import com.rolecompass.exception.ApiException;
import com.rolecompass.repository.AnswerRepository;
import com.rolecompass.repository.QuestionRepository;
import com.rolecompass.repository.SessionRepository;
import com.rolecompass.routing.AdaptiveRoutingEngine;
import com.rolecompass.routing.FsmState;
import com.rolecompass.routing.RoleProfile;
import com.rolecompass.routing.RoutingDecision;
import com.rolecompass.routing.RoutingState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SessionService orchestrates the assessment session lifecycle and batch delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final QuestionService questionService;
    private final AdaptiveRoutingEngine routingEngine;
    private final FeatureAggregationService featureAggregationService;

    // ─── Session Start ────────────────────────────────────────────────────────

    @Transactional
    public SessionStartResponse startSession(User user) {
        Long[] allRoleIds = {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L};
        Double[] emptyVector = new Double[20];
        Arrays.fill(emptyVector, 0.5);
        Boolean[] emptyMask = new Boolean[20];
        Arrays.fill(emptyMask, false);

        Session session = Session.builder()
                .userId(user.getId())
                .status("in_progress")
                .fsmState(FsmState.SECTION_1_RIASEC.name())
                .candidateRoleIds(allRoleIds)
                .answeredVector(emptyVector)
                .answeredDimsMask(emptyMask)
                .build();

        session = sessionRepository.save(session);

        // Fetch first batch of Section 1 questions
        List<QuestionDTO> questions = questionService.getSectionOneFirstBatch(AdaptiveRoutingEngine.BATCH_SIZE);

        log.info("Session {} started for user {}. Returned initial batch of {} Section 1 questions.",
                session.getId(), user.getId(), questions.size());

        return SessionStartResponse.builder()
                .sessionId(session.getId())
                .questions(questions)
                .build();
    }

    // ─── Answer Submission & Next Batch Delivery ──────────────────────────────

    @Transactional
    public Map<String, Object> submitAnswers(UUID sessionId, User user, AnswerRequest request) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ApiException("Session not found or does not belong to user", HttpStatus.NOT_FOUND));

        if ("completed".equals(session.getStatus())) {
            throw new ApiException("Session is already completed.", HttpStatus.BAD_REQUEST);
        }

        // Persist answers
        List<Answer> answers = request.getAnswers().stream()
                .map(item -> Answer.builder()
                        .id(AnswerId.builder()
                                .sessionId(sessionId)
                                .questionId(item.getQuestionId())
                                .build())
                        .likertValue(item.getLikertValue())
                        .build())
                .collect(Collectors.toList());

        answerRepository.saveAll(answers);

        // Retrieve all session answers for state calculation
        List<Answer> allAnswers = answerRepository.findByIdSessionId(sessionId);
        int totalAnswered = allAnswers.size();
        Set<Long> answeredQuestionIds = allAnswers.stream()
                .map(a -> a.getId().getQuestionId())
                .collect(Collectors.toSet());

        // Update internal feature profiles
        Map<String, Double> psychProfile = featureAggregationService.buildPsychProfile(sessionId);
        double[] techVector = featureAggregationService.buildTechVector(sessionId);

        Double[] storedVector = new Double[techVector.length];
        Boolean[] storedMask = new Boolean[techVector.length];
        for (int i = 0; i < techVector.length; i++) {
            storedVector[i] = techVector[i];
            storedMask[i] = (techVector[i] != 0.5);
        }
        session.setAnsweredVector(storedVector);
        session.setAnsweredDimsMask(storedMask);

        // Build routing state & evaluate next decision
        RoutingState routingState = buildRoutingState(session, sessionId, totalAnswered, psychProfile, techVector, answeredQuestionIds);
        RoutingDecision decision = routingEngine.evaluate(routingState);

        log.debug("Routing evaluation for {}: readyToPredict={}, survivors={}, nextState={}, nextQuestions={}",
                sessionId, decision.readyToPredict(), decision.survivingRoles(), decision.nextFsmState(), decision.nextQuestionIds());

        // Update candidate roles and FSM state
        Long[] survivorIds = decision.survivingRoles().stream()
                .map(RoleProfile::idForRoleName)
                .map(Long::valueOf)
                .toArray(Long[]::new);
        session.setCandidateRoleIds(survivorIds);
        session.setFsmState(decision.nextFsmState().name());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("session_id", sessionId);
        response.put("answers_count", totalAnswered);

        if (decision.readyToPredict()) {
            session.setStatus("ready_to_predict");
            response.put("status", "ready_to_predict");
            response.put("questions", List.of());
            response.put("message", "Assessment completed. Ready for prediction.");
        } else {
            session.setStatus("in_progress");
            List<QuestionDTO> nextQuestions = questionService.getQuestionsByIds(decision.nextQuestionIds());
            response.put("status", "in_progress");
            response.put("questions", nextQuestions);
            response.put("next_question_count", nextQuestions.size());
            response.put("message", "Answers recorded. Next question batch delivered.");
        }

        sessionRepository.save(session);
        return response;
    }

    // ─── Prediction Endpoint ──────────────────────────────────────────────────

    @org.springframework.beans.factory.annotation.Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    @Transactional
    public PredictionResponse predict(UUID sessionId, User user) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ApiException("Session not found or does not belong to user", HttpStatus.NOT_FOUND));

        if (!"ready_to_predict".equals(session.getStatus()) && !"completed".equals(session.getStatus())) {
            throw new ApiException(
                    "Session is not ready for prediction. Please complete required assessment questions first.",
                    HttpStatus.BAD_REQUEST);
        }

        // Build 20-dim tech vector from answers
        double[] techVector = featureAggregationService.buildTechVector(sessionId);

        // Surviving candidate role names
        List<String> candidateRoles = Arrays.stream(session.getCandidateRoleIds())
                .map(id -> RoleProfile.nameForId(id.intValue()))
                .collect(Collectors.toList());

        PredictionResponse response;
        try {
            response = callMlService(techVector, candidateRoles);
            log.info("ML service prediction for session {}: role='{}' confidence={}",
                    sessionId, response.getPredictedRole(), response.getConfidence());
        } catch (Exception e) {
            log.warn("ML service unreachable ({}). Using candidate fallback for session {}.",
                    e.getMessage(), sessionId);
            response = fallbackPrediction(candidateRoles);
        }

        session.setStatus("completed");
        session.setPredictedRole(response.getPredictedRole());
        session.setConfidence(response.getConfidence());
        session.setFsmState(FsmState.COMPLETED.name());
        sessionRepository.save(session);

        return response;
    }

    /**
     * POSTs the 20-feature vector and candidate_roles to the FastAPI /score endpoint.
     */
    private PredictionResponse callMlService(double[] techVector, List<String> candidateRoles) {
        org.springframework.web.client.RestTemplate rest = new org.springframework.web.client.RestTemplate();

        // Build request body: { "features": [f0, ..., f19], "candidate_roles": [...] }
        java.util.Map<String, Object> requestBody = new java.util.LinkedHashMap<>();
        Double[] features = new Double[techVector.length];
        for (int i = 0; i < techVector.length; i++) features[i] = techVector[i];
        requestBody.put("features", features);
        requestBody.put("candidate_roles", candidateRoles);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> raw = rest.postForObject(
                mlServiceUrl + "/score", requestBody, java.util.Map.class);

        if (raw == null) throw new RuntimeException("Null response from ML service");

        String predictedRole = (String) raw.get("predicted_role");
        double confidence = ((Number) raw.get("confidence")).doubleValue();

        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> rawAlternates =
                (List<java.util.Map<String, Object>>) raw.get("alternates");

        List<PredictionResponse.AlternateRole> alternates = new ArrayList<>();
        if (rawAlternates != null) {
            for (java.util.Map<String, Object> item : rawAlternates) {
                alternates.add(PredictionResponse.AlternateRole.builder()
                        .role((String) item.get("role"))
                        .confidence(((Number) item.get("confidence")).doubleValue())
                        .build());
            }
        }

        return PredictionResponse.builder()
                .predictedRole(predictedRole)
                .confidence(confidence)
                .alternates(alternates)
                .build();
    }

    private PredictionResponse fallbackPrediction(List<String> candidateRoles) {
        String role = candidateRoles.isEmpty() ? "Backend Developer" : candidateRoles.get(0);
        List<PredictionResponse.AlternateRole> alternates = new ArrayList<>();
        if (candidateRoles.size() > 1) {
            alternates.add(PredictionResponse.AlternateRole.builder()
                    .role(candidateRoles.get(1))
                    .confidence(0.3)
                    .build());
        }
        return PredictionResponse.builder()
                .predictedRole(role)
                .confidence(0.5)
                .alternates(alternates)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(User user) {
        List<Session> userSessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        Map<Long, Question> questionMap = questionRepository.findAll().stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        long completedCount = userSessions.stream()
                .filter(s -> "completed".equalsIgnoreCase(s.getStatus()))
                .count();

        List<Map<String, Object>> sessionHistory = new ArrayList<>();

        for (Session session : userSessions) {
            Map<String, Object> sessionData = new LinkedHashMap<>();
            sessionData.put("session_id", session.getId());
            sessionData.put("status", session.getStatus());
            sessionData.put("fsm_state", session.getFsmState());
            sessionData.put("predicted_role", session.getPredictedRole());
            sessionData.put("confidence", session.getConfidence());
            sessionData.put("created_at", session.getCreatedAt());
            sessionData.put("updated_at", session.getUpdatedAt());

            List<Answer> answers = answerRepository.findByIdSessionId(session.getId());
            sessionData.put("total_questions_answered", answers.size());

            List<Map<String, Object>> qaList = answers.stream()
                    .map(ans -> {
                        Map<String, Object> qa = new LinkedHashMap<>();
                        Long qId = ans.getId().getQuestionId();
                        qa.put("question_id", qId);
                        Question q = questionMap.get(qId);
                        qa.put("question_text", q != null ? q.getText() : "Question #" + qId);
                        qa.put("likert_value", ans.getLikertValue());
                        qa.put("likert_label", getLikertLabel(ans.getLikertValue()));
                        qa.put("answered_at", ans.getCreatedAt());
                        return qa;
                    })
                    .sorted(Comparator.comparing(qa -> (Long) qa.get("question_id")))
                    .collect(Collectors.toList());

            sessionData.put("questions_and_answers", qaList);
            sessionHistory.add(sessionData);
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("user_id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("created_at", user.getCreatedAt());
        profile.put("total_assessments", userSessions.size());
        profile.put("completed_assessments", completedCount);
        profile.put("sessions", sessionHistory);

        return profile;
    }

    private String getLikertLabel(int value) {
        return switch (value) {
            case 1 -> "Strongly Disagree";
            case 2 -> "Disagree";
            case 3 -> "Neutral";
            case 4 -> "Agree";
            case 5 -> "Strongly Agree";
            default -> "Rating: " + value;
        };
    }

    private RoutingState buildRoutingState(
            Session session,
            UUID sessionId,
            int answeredCount,
            Map<String, Double> psychProfile,
            double[] techVector,
            Set<Long> answeredQuestionIds
    ) {
        List<String> candidateNames = Arrays.stream(session.getCandidateRoleIds())
                .map(id -> RoleProfile.nameForId(id.intValue()))
                .collect(Collectors.toList());

        FsmState fsmState = FsmState.SECTION_1_RIASEC;
        if (session.getFsmState() != null) {
            try {
                fsmState = FsmState.valueOf(session.getFsmState());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid fsm_state '{}' in session {}, defaulting to SECTION_1_RIASEC", session.getFsmState(), sessionId);
            }
        }

        return new RoutingState(sessionId, fsmState, candidateNames, psychProfile, techVector, answeredCount, answeredQuestionIds);
    }
}
