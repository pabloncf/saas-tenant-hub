package com.pabloncf.saas.dashboard.member;

import com.pabloncf.saas.common.dto.ApiResponse;
import com.pabloncf.saas.dashboard.member.dto.MemberResponse;
import com.pabloncf.saas.dashboard.member.dto.UpdateRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Members")
@SecurityRequirement(name = "BearerAuth")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all members in the current organization")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> findAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ofPage(memberService.findAll(pageable)));
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Operation(summary = "Update a member's role")
    public ResponseEntity<ApiResponse<MemberResponse>> updateRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.of(memberService.updateRole(userId, request)));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a member from the organization")
    public void removeMember(@PathVariable UUID userId) {
        memberService.removeMember(userId);
    }
}
