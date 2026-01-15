package com.dsi.support.agenticrouter.security;

import com.dsi.support.agenticrouter.entity.AppUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.Objects;

@Getter
public class CustomUserDetails extends User {

    private final transient AppUser user;

    public CustomUserDetails(AppUser user, Collection<? extends GrantedAuthority> authorities) {
        super(
            user.getUsername(),
            user.getPasswordHash(),
            user.isActive(),
            true,
            true,
            true,
            authorities
        );
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomUserDetails that)) return false;

        if (Objects.isNull(this.user) || Objects.isNull(that.user)) return false;

        return Objects.equals(this.user.getId(), that.user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(Objects.nonNull(user) ? user.getId() : null);
    }
}
