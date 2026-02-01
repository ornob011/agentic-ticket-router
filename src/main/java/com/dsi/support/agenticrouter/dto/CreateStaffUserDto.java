package com.dsi.support.agenticrouter.dto;

import com.dsi.support.agenticrouter.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStaffUserDto {

    @NotBlank(message = "{username.required}")
    @Size(max = 50, message = "{username.max}")
    private String username;

    @NotBlank(message = "{email.required}")
    @Email(message = "{email.invalid}")
    @Size(max = 100, message = "{email.max}")
    private String email;

    @NotBlank(message = "{full.name.required}")
    @Size(max = 100, message = "{full.name.max}")
    private String fullName;

    @NotNull(message = "{role.required}")
    private UserRole role;

    @NotBlank(message = "{password.required}")
    @Size(min = 8, max = 128, message = "{password.size}")
    private String password;
}

