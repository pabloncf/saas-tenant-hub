package com.pabloncf.saas.dashboard.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pabloncf.saas.auth.domain.Role;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MemberResponse(
        @JsonProperty("user_id")   UUID userId,
        @JsonProperty("full_name") String fullName,
        String email,
        Role role,
        @JsonProperty("joined_at") OffsetDateTime joinedAt
) {}
