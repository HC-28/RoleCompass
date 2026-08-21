package com.rolecompass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {

    private Long id;
    private String text;

    @Builder.Default
    private List<Integer> options = List.of(1, 2, 3, 4, 5);
}
