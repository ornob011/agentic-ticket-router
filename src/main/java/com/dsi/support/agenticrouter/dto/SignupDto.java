package com.dsi.support.agenticrouter.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupDto {

    @Size(max = 50)
    private String username;

    @Size(max = 100)
    private String email;

    @Size(max = 72)
    private String password;

    private String confirmPassword;

    @Size(max = 100)
    private String fullName;

    @Size(max = 100)
    private String companyName;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String city;

    private String countryIso2;

    private String customerTierCode;
    private String preferredLanguageCode;

}
