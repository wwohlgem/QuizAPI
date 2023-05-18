package com.cooksys.quiz_api.services.impl;

import java.util.*;

import com.cooksys.quiz_api.dtos.QuestionRequestDto;
import com.cooksys.quiz_api.dtos.QuestionResponseDto;
import com.cooksys.quiz_api.dtos.QuizRequestDto;
import com.cooksys.quiz_api.dtos.QuizResponseDto;
import com.cooksys.quiz_api.entities.Answer;
import com.cooksys.quiz_api.entities.Question;
import com.cooksys.quiz_api.entities.Quiz;
import com.cooksys.quiz_api.mappers.AnswerMapper;
import com.cooksys.quiz_api.mappers.QuestionMapper;
import com.cooksys.quiz_api.mappers.QuizMapper;
import com.cooksys.quiz_api.repositories.AnswerRepository;
import com.cooksys.quiz_api.repositories.QuestionRepository;
import com.cooksys.quiz_api.repositories.QuizRepository;
import com.cooksys.quiz_api.services.QuizService;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuizMapper quizMapper;
    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;
    private final AnswerRepository answerRepository;
    private final AnswerMapper answerMapper;

//  private void validateQuizRequest(QuizRequestDto quizRequestDto){
//    if(quizRequestDto.getName() == null || quizRequestDto.getQuestions() == null){
//      throw new BadRequestException; //still need to create this
//    }
//  }


    @Override
    public List<QuizResponseDto> getAllQuizzes() {
        return quizMapper.entitiesToDtos(quizRepository.findAll()); //returns all of the quizes in the table
    }

    @Override
    public QuizResponseDto createQuiz(QuizRequestDto quizRequestDto) {

        Quiz quizToCreate = quizMapper.requestToEntity(quizRequestDto);
        quizRepository.saveAndFlush(quizToCreate);
        //now we have saved the quiz, and have a responseDTO with the id for the quiz

        //for each question in our saved quiz, we need to set the quiz
        //do the same for the list of answers to each question
        //don't forget to save
        for (Question q : quizToCreate.getQuestions()) {
            q.setQuiz(quizToCreate);
            questionRepository.saveAndFlush(q);
            for (Answer a : q.getAnswers()) {
                a.setQuestion(q);
                answerRepository.saveAndFlush(a);
                //nested loop/iteration
            }
        }
        return quizMapper.entityToDto(quizToCreate);
    }

    @Override
    public QuizResponseDto deleteQuiz(Long id) {
        Quiz quizToDelete = quizRepository.getById(id); //not using a dto for a delete
        List<Question> questions = quizToDelete.getQuestions();

        //have to delete answers, THEN questions, THEN quiz
        for (Question q : questions) {

            answerRepository.deleteAll(q.getAnswers());

        }
        questionRepository.deleteAll(questions);
        quizRepository.delete(quizToDelete);

        return quizMapper.entityToDto(quizToDelete);
    }

    @Override
    public QuizResponseDto renameQuiz(Long id, String newName) {
        Quiz quizToRename = quizRepository.getById(id);
        quizToRename.setName(newName);
        quizRepository.saveAndFlush(quizToRename);

        //is this necessary?
//        for(Question q : quizToRename.getQuestions()){
//            q.setQuiz(quizToRename);
//            questionRepository.saveAndFlush(q);
//            for (Answer a : q.getAnswers()){
//                a.setQuestion(q);
//                answerRepository.saveAndFlush(a);
//            }
//        }

        return quizMapper.entityToDto(quizToRename);

    }

    @Override
    public QuestionResponseDto getRandomQuestion(Long id) {
        Quiz quiz = quizRepository.getById(id);

        //need a random number between 1 and number of questions in quiz
        Random random = new Random();

        int randomInt = random.nextInt(quiz.getQuestions().size()) + 1; //add one so we don't get 0

        List<Question> allQuestions = quiz.getQuestions();
        return questionMapper.entityToDto(allQuestions.get(randomInt));

    }

    @Override
    public QuizResponseDto addQuestion(Long id, QuestionRequestDto questionRequestDto) {
        Quiz quizToUpdate = quizRepository.getById(id);
        Question questionToAdd = questionMapper.dtoToEntity(questionRequestDto);

        questionToAdd.setQuiz(quizToUpdate); //establish connection to quiz
        //all of our answers from our questionDto

        questionRepository.saveAndFlush(questionToAdd);

        List<Answer> answers = answerMapper.dtosToEntity(questionRequestDto.getAnswers());

        //set the question to our answers
        for(Answer a : answers){
            a.setQuestion(questionToAdd);
            answerRepository.saveAndFlush(a);
        }


        //add set all of our answers to our question entity
        questionToAdd.setAnswers(answers);

        quizToUpdate.getQuestions().add(questionToAdd);//now we can add it
        quizRepository.saveAndFlush(quizToUpdate);//now save the updated quiz


        return quizMapper.entityToDto(quizToUpdate);
    }

    @Override
    public QuestionResponseDto deleteQuestion(Long id, Long questionID) {
        Quiz quizToDeleteFrom = quizRepository.getById(id);
        List<Question> questions = quizToDeleteFrom.getQuestions();

        //need to use a separate list  to avoid concurrent modification exception
        List<Question> questionsToRemove = new ArrayList<>();

        for (Question q : questions) {
            if (Objects.equals(q.getId(), questionID)) {
                questionsToRemove.add(q);
            }
        }

        questions.removeAll(questionsToRemove);

        quizRepository.saveAndFlush(quizToDeleteFrom);
        return questionMapper.entityToDto(questionsToRemove.get(0));

    }
}
