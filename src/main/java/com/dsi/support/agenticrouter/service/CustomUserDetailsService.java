package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.security.CustomUserDetails;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public CustomUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug(
            "UserDetailsLoad({}) AppUser(usernameLength:{})",
            OperationalLogContext.PHASE_START,
            StringUtils.length(StringUtils.trimToEmpty(username))
        );

        String normalizedUsername = StringUtils.lowerCase(StringUtils.trimToEmpty(username));
        if (StringUtils.isBlank(normalizedUsername)) {
            throw new UsernameNotFoundException("Username is required");
        }

        AppUser user = appUserRepository.findByUsernameIgnoreCase(normalizedUsername)
                                        .orElseThrow(
                                            () -> new UsernameNotFoundException(
                                                String.format("No user found with username: %s", normalizedUsername)
                                            )
                                        );

        if (!user.isActive()) {
            throw new UsernameNotFoundException(
                String.format("User is inactive: %s", normalizedUsername)
            );
        }

        UserDetails userDetails = new CustomUserDetails(
            user,
            buildAuthorities(user.getRole())
        );

        log.debug(
            "UserDetailsLoad({}) AppUser(id:{},username:{},role:{},active:{})",
            OperationalLogContext.PHASE_COMPLETE,
            user.getId(),
            user.getUsername(),
            user.getRole(),
            user.isActive()
        );

        return userDetails;
    }

    private Set<GrantedAuthority> buildAuthorities(
        UserRole role
    ) {
        if (Objects.isNull(role)) {
            return Collections.emptySet();
        }

        return Set.of(
            new SimpleGrantedAuthority(
                String.format("ROLE_%s", role.name())
            )
        );
    }
}
