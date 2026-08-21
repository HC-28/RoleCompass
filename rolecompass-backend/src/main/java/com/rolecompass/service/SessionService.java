package com.rolecompass.service;

import com.rolecompass.dto.*;
import com.rolecompass.entity.Answer;
import com.rolecompass.entity.AnswerId;
import com.rolecompass.entity.Session;
import com.rolecompass.entity.User;
import com.rolecompass.exception.ApiException;
import com.rolecompass.repository.AnswerRepository;
import com.rolecompass.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionService questionService;

    @Transactional
    public SessionStartResponse startSession(User user) {
        Session session = Session.builder()
                .userId(user.getId())
                .status("in_progress")
                .candidateRoleIds(new Long[]{1L, 2L, 3L, 4L})
                .answeredVector(new Double[]{0.0, 0.0, 0.0, 0.0})
                .answeredDimsMask(new Boolean[]{false, false, false, false})
                .build();

        session = sessionRepository.save(session);

        List<QuestionDTO> questions = questionService.getSectionOneQuestions();

        return SessionStartResponse.builder()
                .sessionId(session.getId())
                .questions(questions)
                .build();
    }

    @Transactional
    public Map<String, Object> submitAnswers(UUID sessionId, User user, AnswerRequest request) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ApiException("Session not found or does not belong to user", HttpStatus.NOT_FOUND));

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

        // Demo State Bypass: Override MIN_QUESTIONS = 40 floor, transition immediately to ready_to_predict
        session.setStatus("ready_to_predict");
        sessionRepository.save(session);

        return Map.of(
                "session_id", sessionId,
                "status", "ready_to_predict",
                "answers_count", answers.size(),
                "message", "Answers recorded successfully. Ready for prediction."
        );
    }

    @Transactional
    public PredictionResponse predict(UUID sessionId, User user) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ApiException("Session not found or does not belong to user", HttpStatus.NOT_FOUND));

        if (!"ready_to_predict".equals(session.getStatus()) && !"completed".equals(session.getStatus())) {
            throw new ApiException("Session is not ready for prediction. Please complete required questions first.", HttpStatus.BAD_REQUEST);
        }

        session.setStatus("completed");
        sessionRepository.save(session);

        // ML Layer (Demo Mock): Return exact mock JSON payload
        return PredictionResponse.builder()
                .predictedRole("Backend Developer")
                .confidence(0.88)
                .alternates(List.of(
                        PredictionResponse.AlternateRole.builder()
                                .role("Cloud Engineer")
                                .confidence(0.08)
                                .build()
                ))
                .build();
    }
}
