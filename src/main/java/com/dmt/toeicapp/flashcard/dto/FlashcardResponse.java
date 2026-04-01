package com.dmt.toeicapp.flashcard.dto;

import java.time.OffsetDateTime;

// ── Response ──────────────────────────────────────────────────
public record FlashcardResponse(
        Long           id,
        Long           topicId,
        String         topicName,
        Long           createdById,
        String         createdByUsername,
        String         word,
        String         pronunciation,
        String         definition,
        String         exampleSentence,
        String         imageUrl,
        String         difficulty,
        boolean        isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}