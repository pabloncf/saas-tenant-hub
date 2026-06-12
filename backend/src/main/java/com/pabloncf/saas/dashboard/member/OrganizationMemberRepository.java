package com.pabloncf.saas.dashboard.member;

import com.pabloncf.saas.auth.domain.OrganizationMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    Optional<OrganizationMember> findByUserId(UUID userId);

    // Eagerly fetches user to avoid N+1 on member list
    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<OrganizationMember> findAll(Pageable pageable);
}
