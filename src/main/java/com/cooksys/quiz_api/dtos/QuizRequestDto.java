package com.cooksys.quiz_api.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class QuizRequestDto {

    private String name; //user needs to pass in name
    private List<QuestionRequestDto> questions; //and questions in order to create a quiz


}
