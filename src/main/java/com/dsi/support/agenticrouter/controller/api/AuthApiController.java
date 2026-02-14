package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.SignupDto;
import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.repository.CountryRepository;
import com.dsi.support.agenticrouter.repository.CustomerTierRepository;
import com.dsi.support.agenticrouter.repository.LanguageRepository;
import com.dsi.support.agenticrouter.service.onboarding.SignupService;
import com.dsi.support.agenticrouter.util.Utils;
import com.dsi.support.agenticrouter.validator.SignupValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final SignupService signupService;
    private final SignupValidator signupValidator;
    private final CountryRepository countryRepository;
    private final CustomerTierRepository customerTierRepository;
    private final LanguageRepository languageRepository;
    private final MessageSource messageSource;

    @PostMapping("/login")
    public ApiDtos.UserMe login(
        @Valid @RequestBody ApiDtos.LoginRequest loginRequest,
        HttpServletRequest request
    ) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.username(),
                loginRequest.password()
            )
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            context
        );

        return toUserMe(Utils.getLoggedInUserDetails());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        HttpServletRequest request
    ) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ApiDtos.UserMe me() {
        return toUserMe(
            Utils.getLoggedInUserDetails()
        );
    }

    @GetMapping("/signup-options")
    public ApiDtos.SignupOptionsResponse signupOptions() {
        List<ApiDtos.LookupOption> countries = new ArrayList<>();
        countryRepository.findByActiveTrueOrderByNameAsc()
                         .forEach(country -> countries.add(
                                 new ApiDtos.LookupOption(
                                     country.getIso2(),
                                     country.getName()
                                 )
                             )
                         );

        List<ApiDtos.LookupOption> tiers = new ArrayList<>();
        customerTierRepository.findByActiveTrueOrderByDisplayNameAsc()
                              .forEach(tier -> tiers.add(
                                      new ApiDtos.LookupOption(
                                          tier.getCode(),
                                          tier.getDisplayName()
                                      )
                                  )
                              );

        List<ApiDtos.LookupOption> languages = new ArrayList<>();
        languageRepository.findAllByOrderByNameAsc()
                          .forEach(language -> languages.add(
                                  new ApiDtos.LookupOption(
                                      language.getCode(),
                                      language.getName()
                                  )
                              )
                          );

        return new ApiDtos.SignupOptionsResponse(
            countries,
            tiers,
            languages
        );
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
        @Valid @RequestBody ApiDtos.SignupRequest signupRequest
    ) {
        SignupDto signupDto = new SignupDto();

        signupDto.setUsername(signupRequest.username());
        signupDto.setEmail(signupRequest.email());
        signupDto.setPassword(signupRequest.password());
        signupDto.setConfirmPassword(signupRequest.confirmPassword());
        signupDto.setFullName(signupRequest.fullName());
        signupDto.setCompanyName(signupRequest.companyName());
        signupDto.setPhoneNumber(signupRequest.phoneNumber());
        signupDto.setAddress(signupRequest.address());
        signupDto.setCity(signupRequest.city());
        signupDto.setCountryIso2(signupRequest.countryIso2());
        signupDto.setCustomerTierCode(signupRequest.customerTierCode());
        signupDto.setPreferredLanguageCode(signupRequest.preferredLanguageCode());

        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(
            signupDto,
            "signup"
        );

        signupValidator.validate(signupDto, errors);

        if (errors.hasErrors()) {
            List<ApiDtos.ValidationFieldError> fieldErrors = new ArrayList<>();
            errors.getFieldErrors()
                  .forEach(fieldError -> fieldErrors.add(
                          new ApiDtos.ValidationFieldError(
                              fieldError.getField(),
                              Utils.getMessageFromMessageSource(messageSource, fieldError)
                          )
                      )
                  );

            List<String> globalErrors = new ArrayList<>();
            errors.getGlobalErrors()
                  .forEach(error -> globalErrors.add(
                          Utils.getMessageFromMessageSource(messageSource, error)
                      )
                  );

            List<String> allErrors = new ArrayList<>();
            errors.getAllErrors()
                  .forEach(error -> allErrors.add(
                          Utils.getMessageFromMessageSource(messageSource, error)
                      )
                  );

            return ResponseEntity.badRequest().body(
                new ApiDtos.ValidationErrorResponse(
                    fieldErrors,
                    globalErrors,
                    allErrors
                )
            );
        }

        signupService.signupCustomer(signupDto);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private ApiDtos.UserMe toUserMe(AppUser appUser) {
        return new ApiDtos.UserMe(
            appUser.getId(),
            appUser.getUsername(),
            appUser.getFullName(),
            appUser.getRole()
        );
    }
}
