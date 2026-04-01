package com.dmt.toeicapp.progress.service;

import com.dmt.toeicapp.progress.dto.ProgressResponse;
import com.dmt.toeicapp.progress.dto.ReviewRequest;

import java.util.List;

public interface ProgressService {

    // Lấy toàn bộ progress của user hiện tại
    List<ProgressResponse> getMyProgress();

    // Lấy danh sách card cần ôn hôm nay (next_review_at <= now)
    List<ProgressResponse> getDueCards();

    // Submit kết quả review — tính lại SM-2
    ProgressResponse review(ReviewRequest request);
}