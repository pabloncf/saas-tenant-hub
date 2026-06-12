package com.pabloncf.saas.dashboard.project;

import com.pabloncf.saas.common.domain.Organization;
import com.pabloncf.saas.common.domain.OrganizationRepository;
import com.pabloncf.saas.dashboard.activity.ActivityLogService;
import com.pabloncf.saas.dashboard.project.dto.ProjectRequest;
import com.pabloncf.saas.dashboard.project.dto.ProjectResponse;
import com.pabloncf.saas.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository     projectRepository;
    private final ProjectMapper         projectMapper;
    private final ActivityLogService    activityLogService;
    private final OrganizationRepository orgRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMapper projectMapper,
                          ActivityLogService activityLogService,
                          OrganizationRepository orgRepository) {
        this.projectRepository  = projectRepository;
        this.projectMapper      = projectMapper;
        this.activityLogService = activityLogService;
        this.orgRepository      = orgRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> findAll(ProjectStatus status, String search, Pageable pageable) {
        Specification<Project> spec = Specification
                .where(ProjectSpecification.hasStatus(status))
                .and(ProjectSpecification.nameContains(search));
        return projectRepository.findAll(spec, pageable).map(projectMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(UUID id) {
        return projectRepository.findById(id)
                .map(projectMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Organization org = orgRepository.findById(TenantContext.getCurrentTenant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        long projectCount = projectRepository.count();
        if (!org.getSubscriptionTier().allowsMoreProjects(projectCount)) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Project limit reached for " + org.getSubscriptionTier() + " plan. Upgrade to create more projects.");
        }

        Project project = projectMapper.toEntity(request);
        project.initTenant(TenantContext.getCurrentTenant());
        Project saved = projectRepository.save(project);
        activityLogService.log(saved.getId(), "PROJECT_CREATED", Map.of("name", saved.getName()));
        return projectMapper.toResponse(saved);
    }

    @Transactional
    public ProjectResponse update(UUID id, ProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        projectMapper.updateEntity(request, project);
        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        projectRepository.delete(project);
        activityLogService.log(null, "PROJECT_DELETED", Map.of("id", id.toString(), "name", project.getName()));
    }
}
