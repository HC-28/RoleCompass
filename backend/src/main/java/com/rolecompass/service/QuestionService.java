package com.rolecompass.service;

import com.rolecompass.dto.response.QuestionDTO;
import com.rolecompass.entity.Question;
import com.rolecompass.repository.QuestionRepository;
import com.rolecompass.routing.SectionMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * QuestionService — retrieval and transformation of Question entities into
 * client-facing {@link QuestionDTO}s.
 *
 * <p><strong>Dumb-Client Rule:</strong> Dimension tags and trigger predicates
 * are stripped during transformation. Only text, options, and section-context
 * metadata are returned to the frontend.</p>
 */
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    public List<QuestionDTO> getSectionOneFirstBatch(int batchSize) {
        return questionRepository.findBySectionIdOrderByIdAsc(1)
                .stream()
                .limit(batchSize)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<QuestionDTO> getQuestionsByIds(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        // Build an ordered lookup list so we preserve the routing engine's intended order.
        // findAllById does NOT guarantee insertion-order, so we re-sort to match questionIds.
        List<Long> orderedIds = questionIds instanceof List ? (List<Long>) questionIds : new java.util.ArrayList<>(questionIds);
        Map<Long, Question> byId = questionRepository.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        return orderedIds.stream()
                .filter(byId::containsKey)
                .map(id -> toDTO(byId.get(id)))
                .collect(Collectors.toList());
    }

    public List<QuestionDTO> getNextUnansweredSectionOneQuestions(Set<Long> answeredIds, int batchSize) {
        return questionRepository.findBySectionIdOrderByIdAsc(1)
                .stream()
                .filter(q -> !answeredIds.contains(q.getId()))
                .limit(batchSize)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<QuestionDTO> getSectionOneQuestions() {
        return questionRepository.findBySectionIdOrderByIdAsc(1)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ─── DTO Assembly ─────────────────────────────────────────────────────────

    /**
     * Converts a Question entity to a client-facing DTO, injecting section and
     * subsection labels from {@link SectionMetadata}. Internal tags are excluded.
     */
    private QuestionDTO toDTO(Question q) {
        int sectionId = q.getSectionId();
        String primaryTag = (q.getDimensionTags() != null && q.getDimensionTags().length > 0)
                ? q.getDimensionTags()[0]
                : null;

        return QuestionDTO.builder()
                .id(q.getId())
                .text(q.getText())
                .options(List.of(1, 2, 3, 4, 5))
                .responseType(q.getResponseType() != null ? q.getResponseType() : "LIKERT_5")
                .sectionNumber(sectionId)
                .sectionLabel(SectionMetadata.SECTION_LABELS.get(sectionId))
                .subsectionLabel(SectionMetadata.resolveSubsectionLabel(sectionId, primaryTag))
                .build();
    }
}
