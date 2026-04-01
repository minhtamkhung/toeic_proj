package com.dmt.toeicapp.topic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

// ── Response ──────────────────────────────────────────────────
// Record đọc-only, Jackson tự serialize được
public record TopicResponse(
        Long            id,
        String          name,
        String          description,
        String          iconUrl,
        int             displayOrder,
        @JsonProperty("isSystem")
        boolean         isSystem,
        Long            createdById,
        String          createdByUsername,
        OffsetDateTime  createdAt
) {}