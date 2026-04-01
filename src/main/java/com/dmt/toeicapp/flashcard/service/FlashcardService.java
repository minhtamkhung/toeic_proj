package com.dmt.toeicapp.flashcard.service;

import com.dmt.toeicapp.flashcard.dto.FlashcardRequest;
import com.dmt.toeicapp.flashcard.dto.FlashcardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface FlashcardService {

    // Lấy tất cả flashcard user được thấy (có phân trang)
    Page<FlashcardResponse> getAccessible(Pageable pageable);

    // Lấy flashcard theo topic (kiểm tra quyền truy cập topic trong service)
    Page<FlashcardResponse> getByTopic(Long topicId, Pageable pageable);

    // Lấy 1 flashcard theo id
    FlashcardResponse getById(Long id);

    // Tạo mới — không kèm ảnh, upload ảnh riêng qua endpoint /image
    FlashcardResponse create(FlashcardRequest request);

    // Cập nhật nội dung — chỉ owner hoặc ADMIN
    FlashcardResponse update(Long id, FlashcardRequest request);

    // Xóa mềm (soft delete) — chỉ owner hoặc ADMIN
    void delete(Long id);

    // Upload / thay ảnh lên Cloudinary
    FlashcardResponse uploadImage(Long id, MultipartFile file);

    // Xóa ảnh khỏi Cloudinary và clear imageUrl trong DB
    FlashcardResponse deleteImage(Long id);
}