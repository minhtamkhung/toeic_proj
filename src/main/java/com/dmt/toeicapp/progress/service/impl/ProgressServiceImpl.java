package com.dmt.toeicapp.progress.service.impl;

import com.dmt.toeicapp.common.exception.AppException;
import com.dmt.toeicapp.common.security.SecurityUtils;
import com.dmt.toeicapp.common.util.SM2Algorithm;
import com.dmt.toeicapp.flashcard.entity.Flashcard;
import com.dmt.toeicapp.flashcard.repository.FlashcardRepository;
import com.dmt.toeicapp.progress.dto.ProgressResponse;
import com.dmt.toeicapp.progress.dto.ReviewRequest;
import com.dmt.toeicapp.progress.entity.UserProgress;
import com.dmt.toeicapp.progress.mapper.ProgressMapper;
import com.dmt.toeicapp.progress.repository.UserProgressRepository;
import com.dmt.toeicapp.progress.service.ProgressService;
import com.dmt.toeicapp.user.entity.User;
import com.dmt.toeicapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private final UserProgressRepository progressRepository;
    private final FlashcardRepository    flashcardRepository;
    private final UserRepository         userRepository;
    private final ProgressMapper         progressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProgressResponse> getMyProgress() {
        Long userId = SecurityUtils.getCurrentUserId();
        return progressRepository.findByUserId(userId)
                .stream()
                .map(progressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgressResponse> getDueCards() {
        Long userId = SecurityUtils.getCurrentUserId();
        return progressRepository.findDueForReview(userId, OffsetDateTime.now())
                .stream()
                .map(progressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProgressResponse review(ReviewRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // Lấy flashcard — kiểm tra tồn tại
        Flashcard flashcard = flashcardRepository
                .findByIdAndIsActiveTrue(request.flashcardId())
                .orElseThrow(() -> AppException.notFound(
                        "Không tìm thấy flashcard với id = " + request.flashcardId()));

        // Lấy hoặc tạo mới progress record
        UserProgress progress = progressRepository
                .findByUserIdAndFlashcardId(currentUserId, request.flashcardId())
                .orElseGet(() -> createNewProgress(currentUserId, flashcard));

        // Tính SM-2
        SM2Algorithm.Result result = SM2Algorithm.calculate(
                request.quality(),
                progress.getSm2Repetitions(),
                progress.getEasinessFactor(),
                progress.getIntervalDays()
        );

        // Cập nhật progress
        progress.setReviewCount(progress.getReviewCount() + 1);
        progress.setLastReviewedAt(OffsetDateTime.now());
        progress.setSm2Repetitions(result.newRepetitions());
        progress.setEasinessFactor(result.newEasinessFactor());
        progress.setIntervalDays(result.newIntervalDays());
        progress.setNextReviewAt(result.nextReviewAt());

        if (request.quality() >= 3) {
            progress.setCorrectCount(progress.getCorrectCount() + 1);
        }

        // Cập nhật status dựa trên số lần đúng liên tiếp
        progress.setStatus(determineStatus(result.newRepetitions()));

        log.debug("SM-2 review: user={}, card={}, quality={}, nextReview={}",
                currentUserId, request.flashcardId(),
                request.quality(), result.nextReviewAt());

        return progressMapper.toResponse(progressRepository.save(progress));
    }

    // ── Private helpers ───────────────────────────────────────

    private UserProgress createNewProgress(Long userId, Flashcard flashcard) {
        User user = userRepository.getReferenceById(userId);
        return UserProgress.builder()
                .user(user)
                .flashcard(flashcard)
                .build();
        // Các giá trị mặc định đã được set trong @Builder.Default của entity
    }

    // Xác định status dựa trên số lần đúng liên tiếp (sm2Repetitions)
    private UserProgress.Status determineStatus(int repetitions) {
        if (repetitions == 0)       return UserProgress.Status.LEARNING;
        if (repetitions <= 2)       return UserProgress.Status.REVIEWING;
        return                             UserProgress.Status.MASTERED;
    }
}