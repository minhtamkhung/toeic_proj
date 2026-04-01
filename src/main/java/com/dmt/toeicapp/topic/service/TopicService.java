package com.dmt.toeicapp.topic.service;

import com.dmt.toeicapp.topic.dto.TopicRequest;
import com.dmt.toeicapp.topic.dto.TopicResponse;

import java.util.List;

public interface TopicService {

    // Lấy tất cả topic user được thấy (system + personal của mình)
    List<TopicResponse> getAccessible();

    // Lấy 1 topic theo id (kiểm tra quyền truy cập)
    TopicResponse getById(Long id);

    // Tạo topic mới — ADMIN tạo system topic, USER tạo personal topic
    TopicResponse create(TopicRequest request);

    // Cập nhật — chỉ owner hoặc ADMIN
    TopicResponse update(Long id, TopicRequest request);

    // Xóa — chỉ owner hoặc ADMIN
    void delete(Long id);
}