package com.pabloncf.saas.dashboard.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pabloncf.saas.dashboard.project.ProjectStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        ProjectStatus status,
        @JsonProperty("created_by") UUID createdBy,
        @JsonProperty("updated_by") UUID updatedBy,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt
) {}
