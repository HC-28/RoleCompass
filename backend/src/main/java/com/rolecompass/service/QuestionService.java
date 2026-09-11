package com.rolecompass.service;

import com.rolecompass.dto.QuestionDTO;
import com.rolecompass.entity.Question;
import com.rolecompass.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * QuestionService — handles retrieval and transformation of Question entities into client-facing QuestionDTOs.
 *
 * <p><strong>Dumb Client Rule:</strong> Dimension tags, trigger predicates, and internal metadata
 * are stripped during transformation and never returned to the frontend.</p>
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
        return questionRepository.findAllById(questionIds)
                .stream()
                .map(this::toDTO)
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

    private QuestionDTO toDTO(Question q) {
        return QuestionDTO.builder()
                .id(q.getId())
                .text(q.getText())
                .options(List.of(1, 2, 3, 4, 5))
                .build();
    }
}
