package com.pabloncf.saas.dashboard.project;

import com.pabloncf.saas.dashboard.project.dto.ProjectRequest;
import com.pabloncf.saas.dashboard.project.dto.ProjectResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectResponse toResponse(Project project);

    // tenantId is intentionally excluded — set by ProjectService from TenantContext
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Project toEntity(ProjectRequest request);

    // Partial update: null fields in request leave the existing value unchanged
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(ProjectRequest request, @MappingTarget Project project);
}
