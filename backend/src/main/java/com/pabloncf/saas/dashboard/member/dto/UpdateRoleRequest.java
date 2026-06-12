package com.pabloncf.saas.dashboard.member.dto;

import com.pabloncf.saas.auth.domain.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull Role role) {}
