package com.dmt.toeicapp.progress.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// ── ProgressResponse ──────────────────────────────────────────
public record ProgressResponse(
        Long           id,
        Long           flashcardId,
        String         flashcardWord,
        String         status,
        int            reviewCount,
        int            correctCount,
        BigDecimal     easinessFactor,
        int            intervalDays,
        int            sm2Repetitions,
        OffsetDateTime lastReviewedAt,
        OffsetDateTime nextReviewAt
) {}