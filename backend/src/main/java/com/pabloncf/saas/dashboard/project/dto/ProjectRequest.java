package com.pabloncf.saas.dashboard.project.dto;

import com.pabloncf.saas.dashboard.project.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        ProjectStatus status
) {}
