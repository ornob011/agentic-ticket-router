package com.dsi.support.agenticrouter.service.auth;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthCurrentUserService {

    private final AppUserRepository appUserRepository;

    public AppUser requireCurrentUser() {
        Long currentUserId = Utils.getLoggedInUserId();

        return appUserRepository.findById(currentUserId)
                                .orElseThrow(
                                    DataNotFoundException.supplier(
                                        AppUser.class,
                                        currentUserId
                                    )
                                );
    }
}
