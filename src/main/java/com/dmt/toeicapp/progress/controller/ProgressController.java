package com.dmt.toeicapp.progress.controller;

import com.dmt.toeicapp.common.response.ApiResponse;
import com.dmt.toeicapp.progress.dto.ProgressResponse;
import com.dmt.toeicapp.progress.dto.ReviewRequest;
import com.dmt.toeicapp.progress.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    // Lấy toàn bộ progress của user hiện tại
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ProgressResponse>>> getMyProgress() {
        return ResponseEntity.ok(
                ApiResponse.ok(progressService.getMyProgress())
        );
    }

    // Lấy danh sách card cần ôn hôm nay — gọi mỗi khi mở app học
    @GetMapping("/due")
    public ResponseEntity<ApiResponse<List<ProgressResponse>>> getDueCards() {
        return ResponseEntity.ok(
                ApiResponse.ok(progressService.getDueCards(),
                        "Danh sách card cần ôn hôm nay")
        );
    }

    // Submit kết quả review một card
    @PostMapping("/review")
    public ResponseEntity<ApiResponse<ProgressResponse>> review(
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(progressService.review(request),
                        "Đã ghi nhận kết quả review")
        );
    }
}