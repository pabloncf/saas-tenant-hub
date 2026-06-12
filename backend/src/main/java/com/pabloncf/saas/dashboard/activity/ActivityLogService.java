package com.pabloncf.saas.dashboard.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pabloncf.saas.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);

    private final ActivityLogRepository repository;
    private final ObjectMapper objectMapper;

    public ActivityLogService(ActivityLogRepository repository, ObjectMapper objectMapper) {
        this.repository   = repository;
        this.objectMapper = objectMapper;
    }

    public void log(UUID projectId, String action, Map<String, Object> detail) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID userId)) {
            log.warn("Activity log skipped — no authenticated user in context for action '{}'", action);
            return;
        }

        String detailJson = null;
        if (detail != null) {
            try {
                detailJson = objectMapper.writeValueAsString(detail);
            } catch (JsonProcessingException e) {
                detailJson = "{}";
            }
        }

        repository.save(new ActivityLog(
                TenantContext.getCurrentTenant(),
                projectId,
                userId,
                action,
                detailJson
        ));
    }
}
