package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.SignupDto;
import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.service.auth.AuthQueryService;
import com.dsi.support.agenticrouter.service.auth.ProfileService;
import com.dsi.support.agenticrouter.service.onboarding.SignupService;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import com.dsi.support.agenticrouter.util.Utils;
import com.dsi.support.agenticrouter.validator.SignupValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final AuthQueryService authQueryService;
    private final SignupService signupService;
    private final SignupValidator signupValidator;
    private final MessageSource messageSource;
    private final ProfileService profileService;
    private final RememberMeServices rememberMeServices;

    @PostMapping("/login")
    public ApiDtos.UserMe login(
        @Valid @RequestBody ApiDtos.LoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                normalizeUsername(
                    loginRequest.username()
                ),
                loginRequest.password()
            )
        );

        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            context
        );

        if (loginRequest.rememberMe()) {
            rememberMeServices.loginSuccess(
                request,
                response,
                authentication
            );
        } else {
            rememberMeServices.loginFail(
                request,
                response
            );
        }

        return authQueryService.toUserMe(
            Utils.getLoggedInUserDetails()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        rememberMeServices.loginFail(
            request,
            response
        );

        HttpSession session = request.getSession(false);

        if (Objects.nonNull(session)) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ApiDtos.UserMe me() {
        return authQueryService.toUserMe(
            Utils.getLoggedInUserDetails()
        );
    }

    @GetMapping("/profile")
    public ApiDtos.ProfileResponse profile() {
        return profileService.getMyProfile();
    }

    @PutMapping("/profile")
    public ApiDtos.ProfileResponse updateProfile(
        @Valid @RequestBody ApiDtos.ProfileUpdateRequest request
    ) throws BindException {
        return profileService.updateMyProfile(request);
    }

    @GetMapping("/settings")
    public ApiDtos.UserSettingsResponse settings() {
        return profileService.getMySettings();
    }

    @PutMapping("/settings")
    public ApiDtos.UserSettingsResponse updateSettings(
        @Valid @RequestBody ApiDtos.UserSettingsUpdateRequest request
    ) {
        return profileService.updateMySettings(request);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
        @Valid @RequestBody ApiDtos.ChangePasswordRequest request
    ) throws BindException {
        profileService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/signup-options")
    public ApiDtos.SignupOptionsResponse signupOptions() {
        return authQueryService.signupOptions();
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
        @Valid @RequestBody ApiDtos.SignupRequest signupRequest
    ) {
        SignupDto signupDto = toSignupDto(
            signupRequest
        );

        BindingResult errors = BindValidation.bindingResult(
            signupDto,
            "signup"
        );

        signupValidator.validate(signupDto, errors);

        if (errors.hasErrors()) {
            ApiDtos.ValidationErrorResponse validationErrorResponse = toValidationErrorResponse(
                errors
            );
            return ResponseEntity.badRequest().body(validationErrorResponse);
        }

        signupService.signupCustomer(signupDto);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private SignupDto toSignupDto(
        ApiDtos.SignupRequest signupRequest
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

        return signupDto;
    }

    private ApiDtos.ValidationErrorResponse toValidationErrorResponse(
        BindingResult errors
    ) {
        List<ApiDtos.ValidationFieldError> fieldErrors = new ArrayList<>();
        errors.getFieldErrors()
              .forEach(fieldError -> {
                  ApiDtos.ValidationFieldError validationFieldError = ApiDtos.ValidationFieldError.builder()
                                                                                                  .field(fieldError.getField())
                                                                                                  .message(
                                                                                                      Utils.getMessageFromMessageSource(
                                                                                                          messageSource,
                                                                                                          fieldError
                                                                                                      )
                                                                                                  )
                                                                                                  .build();
                  fieldErrors.add(validationFieldError);
              });

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

        return ApiDtos.ValidationErrorResponse.builder()
                                             .fieldErrors(fieldErrors)
                                             .globalErrors(globalErrors)
                                             .errors(allErrors)
                                             .build();
    }

    private String normalizeUsername(
        String username
    ) {
        return StringNormalizationUtils.lowerTrimmedOrEmpty(username);
    }
}
