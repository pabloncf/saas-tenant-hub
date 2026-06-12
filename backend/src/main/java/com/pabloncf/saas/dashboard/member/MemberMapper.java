package com.pabloncf.saas.dashboard.member;

import com.pabloncf.saas.auth.domain.OrganizationMember;
import com.pabloncf.saas.dashboard.member.dto.MemberResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    @Mapping(source = "user.id",       target = "userId")
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.email",    target = "email")
    @Mapping(source = "createdAt",     target = "joinedAt")
    MemberResponse toResponse(OrganizationMember member);
}
