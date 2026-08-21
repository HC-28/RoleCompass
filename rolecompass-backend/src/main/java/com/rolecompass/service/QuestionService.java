package com.rolecompass.service;

import com.rolecompass.dto.QuestionDTO;
import com.rolecompass.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    public List<QuestionDTO> getSectionOneQuestions() {
        return questionRepository.findBySectionIdOrderByIdAsc(1)
                .stream()
                .map(q -> QuestionDTO.builder()
                        .id(q.getId())
                        .text(q.getText())
                        .options(List.of(1, 2, 3, 4, 5))
                        .build())
                .collect(Collectors.toList());
    }
}
