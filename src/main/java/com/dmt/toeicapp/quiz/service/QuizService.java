package com.dmt.toeicapp.quiz.service;

import com.dmt.toeicapp.quiz.dto.QuizAnswerRequest;
import com.dmt.toeicapp.quiz.dto.QuizAnswerResponse;
import com.dmt.toeicapp.quiz.dto.QuizAttemptSummary;
import com.dmt.toeicapp.quiz.dto.QuizReviewResponse;
import com.dmt.toeicapp.quiz.dto.QuizStartRequest;

import java.util.List;

public interface QuizService {

    // Tạo quiz mới — trả về danh sách câu hỏi + đáp án đã shuffle
    QuizAttemptSummary start(QuizStartRequest request);

    // Submit đáp án cho 1 câu — trả về đúng/sai + đáp án đúng
    QuizAnswerResponse answer(Long attemptId, QuizAnswerRequest request);

    // Kết thúc quiz — tính điểm, lưu thời gian
    QuizAttemptSummary finish(Long attemptId);

    // Lịch sử quiz của user
    List<QuizAttemptSummary> getHistory();

    // Xem lại câu sai
    QuizReviewResponse review(Long attemptId);
}