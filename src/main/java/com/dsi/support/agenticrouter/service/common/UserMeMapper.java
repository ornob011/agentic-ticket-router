package com.dsi.support.agenticrouter.service.common;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import org.springframework.stereotype.Service;

@Service
public class UserMeMapper {

    public ApiDtos.UserMe toUserMe(
        AppUser appUser
    ) {
        return ApiDtos.UserMe.builder()
                             .id(appUser.getId())
                             .username(appUser.getUsername())
                             .email(appUser.getEmail())
                             .fullName(appUser.getFullName())
                             .role(appUser.getRole())
                             .roleLabel(EnumDisplayNameResolver.resolve(
                                 appUser.getRole()
                             ))
                             .build();
    }
}
