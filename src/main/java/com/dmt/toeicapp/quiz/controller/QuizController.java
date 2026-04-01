package com.dmt.toeicapp.quiz.controller;

import com.dmt.toeicapp.common.response.ApiResponse;
import com.dmt.toeicapp.quiz.dto.*;
import com.dmt.toeicapp.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // Bắt đầu quiz mới
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<QuizAttemptSummary>> start(
            @Valid @RequestBody QuizStartRequest request) {
        return ResponseEntity.status(201).body(
                ApiResponse.created(quizService.start(request))
        );
    }

    // Submit đáp án 1 câu
    @PostMapping("/{attemptId}/answer")
    public ResponseEntity<ApiResponse<QuizAnswerResponse>> answer(
            @PathVariable Long attemptId,
            @Valid @RequestBody QuizAnswerRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(quizService.answer(attemptId, request))
        );
    }

    // Kết thúc quiz
    @PostMapping("/{attemptId}/finish")
    public ResponseEntity<ApiResponse<QuizAttemptSummary>> finish(
            @PathVariable Long attemptId) {
        return ResponseEntity.ok(
                ApiResponse.ok(quizService.finish(attemptId), "Quiz kết thúc!")
        );
    }

    // Lịch sử quiz
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<QuizAttemptSummary>>> getHistory() {
        return ResponseEntity.ok(
                ApiResponse.ok(quizService.getHistory())
        );
    }

    // Xem lại câu sai
    @GetMapping("/{attemptId}/review")
    public ResponseEntity<ApiResponse<QuizReviewResponse>> review(
            @PathVariable Long attemptId) {
        return ResponseEntity.ok(
                ApiResponse.ok(quizService.review(attemptId))
        );
    }
}