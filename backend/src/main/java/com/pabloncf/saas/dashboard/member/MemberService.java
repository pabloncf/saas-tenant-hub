package com.pabloncf.saas.dashboard.member;

import com.pabloncf.saas.auth.domain.OrganizationMember;
import com.pabloncf.saas.auth.domain.Role;
import com.pabloncf.saas.dashboard.member.dto.MemberResponse;
import com.pabloncf.saas.dashboard.member.dto.UpdateRoleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class MemberService {

    private final OrganizationMemberRepository memberRepository;
    private final MemberMapper memberMapper;

    public MemberService(OrganizationMemberRepository memberRepository, MemberMapper memberMapper) {
        this.memberRepository = memberRepository;
        this.memberMapper     = memberMapper;
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> findAll(Pageable pageable) {
        return memberRepository.findAll(pageable).map(memberMapper::toResponse);
    }

    @Transactional
    public MemberResponse updateRole(UUID targetUserId, UpdateRoleRequest request) {
        UUID currentUserId = currentUserId();
        if (currentUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot change your own role");
        }

        OrganizationMember member = findMember(targetUserId);

        if (member.getRole() == Role.OWNER && request.role() != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot demote the organization owner");
        }

        member.setRole(request.role());
        return memberMapper.toResponse(memberRepository.save(member));
    }

    @Transactional
    public void removeMember(UUID targetUserId) {
        UUID currentUserId = currentUserId();
        if (currentUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot remove yourself from the organization");
        }

        OrganizationMember member = findMember(targetUserId);

        if (member.getRole() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot remove the organization owner");
        }

        memberRepository.delete(member);
    }

    private OrganizationMember findMember(UUID userId) {
        return memberRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UUID) auth.getPrincipal();
    }
}
