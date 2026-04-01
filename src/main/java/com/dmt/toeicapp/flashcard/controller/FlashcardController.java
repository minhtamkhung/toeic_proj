package com.dmt.toeicapp.flashcard.controller;

import com.dmt.toeicapp.common.response.ApiResponse;
import com.dmt.toeicapp.flashcard.dto.FlashcardRequest;
import com.dmt.toeicapp.flashcard.dto.FlashcardResponse;
import com.dmt.toeicapp.flashcard.service.FlashcardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;

    // GET /api/flashcards?page=0&size=20&sort=createdAt,desc
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FlashcardResponse>>> getAccessible(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.ok(flashcardService.getAccessible(pageable))
        );
    }

    // GET /api/flashcards?topicId=1&page=0&size=20
    @GetMapping(params = "topicId")
    public ResponseEntity<ApiResponse<Page<FlashcardResponse>>> getByTopic(
            @RequestParam Long topicId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.ok(flashcardService.getByTopic(topicId, pageable))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlashcardResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(flashcardService.getById(id))
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FlashcardResponse>> create(
            @Valid @RequestBody FlashcardRequest request) {
        return ResponseEntity.status(201).body(
                ApiResponse.created(flashcardService.create(request))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FlashcardResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody FlashcardRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(flashcardService.update(id, request), "Cập nhật flashcard thành công")
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        flashcardService.delete(id);
        return ResponseEntity.noContent().build(); // 204
    }

    // POST /api/flashcards/{id}/image  — multipart/form-data
    @PostMapping("/{id}/image")
    public ResponseEntity<ApiResponse<FlashcardResponse>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                ApiResponse.ok(flashcardService.uploadImage(id, file), "Upload ảnh thành công")
        );
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<ApiResponse<FlashcardResponse>> deleteImage(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(flashcardService.deleteImage(id), "Xóa ảnh thành công")
        );
    }
}