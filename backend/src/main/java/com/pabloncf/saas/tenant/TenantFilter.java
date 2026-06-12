package com.pabloncf.saas.tenant;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class TenantFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    // Phase 3 will replace this header with the tenant claim extracted from the JWT
    static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantHeader = httpRequest.getHeader(TENANT_HEADER);

        try {
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                TenantContext.setCurrentTenant(UUID.fromString(tenantHeader));
            }
            chain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            log.warn("Rejected request with malformed tenant ID: '{}'", tenantHeader);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
