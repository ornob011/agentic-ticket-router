package com.dsi.support.agenticrouter.validator;

import com.dsi.support.agenticrouter.dto.SignupDto;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.CountryRepository;
import com.dsi.support.agenticrouter.repository.CustomerTierRepository;
import com.dsi.support.agenticrouter.repository.LanguageRepository;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class SignupValidator implements Validator {

    private final AppUserRepository appUserRepository;
    private final CountryRepository countryRepository;
    private final CustomerTierRepository customerTierRepository;
    private final LanguageRepository languageRepository;

    @Override
    public boolean supports(
        @NonNull Class<?> clazz
    ) {
        return SignupDto.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(
        @NonNull Object target,
        @NonNull Errors errors
    ) {
        if (!(target instanceof SignupDto signupDto)) {
            errors.reject("error.signup.invalid");
            return;
        }

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username", "error.signup.username.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "error.signup.email.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "error.signup.password.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "confirmPassword", "error.signup.confirmPassword.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "fullName", "error.signup.fullName.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "companyName", "error.signup.companyName.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "phoneNumber", "error.signup.phoneNumber.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "address", "error.signup.address.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "city", "error.signup.city.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "countryIso2", "error.signup.countryIso2.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "customerTierCode", "error.signup.customerTierCode.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "preferredLanguageCode", "error.signup.preferredLanguageCode.empty");

        if (errors.hasErrors()) {
            return;
        }

        if (!signupDto.getPassword().equals(signupDto.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "error.signup.password.mismatch");
        }

        String rawPassword = signupDto.getPassword();
        if (rawPassword.length() < 8) {
            errors.rejectValue("password", "error.signup.password.tooShort");
        } else {
            boolean containsLetter = rawPassword.chars()
                                                .anyMatch(Character::isLetter);

            boolean containsDigit = rawPassword.chars()
                                               .anyMatch(Character::isDigit);

            if (!(containsLetter && containsDigit)) {
                errors.rejectValue("password", "error.signup.password.weak");
            }
        }

        String normalizedEmail = StringNormalizationUtils.lowerTrimmedOrEmpty(signupDto.getEmail());

        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            errors.rejectValue("email", "error.signup.email.exists");
        }

        String normalizedUsername = StringNormalizationUtils.lowerTrimmedOrEmpty(signupDto.getUsername());

        if (appUserRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            errors.rejectValue("username", "error.signup.username.exists");
        }

        String normalizedCountryIso2 = StringNormalizationUtils.upperTrimmedOrEmpty(signupDto.getCountryIso2());

        if (!countryRepository.existsByIso2IgnoreCase(normalizedCountryIso2)) {
            errors.rejectValue("countryIso2", "error.signup.countryIso2.invalid");
        }

        String normalizedTierCode = StringNormalizationUtils.trimToEmpty(signupDto.getCustomerTierCode());

        if (!customerTierRepository.existsByCode(normalizedTierCode)) {
            errors.rejectValue("customerTierCode", "error.signup.customerTierCode.invalid");
        }

        String normalizedLanguageCode = StringNormalizationUtils.trimToEmpty(signupDto.getPreferredLanguageCode());

        if (!languageRepository.existsByCode(normalizedLanguageCode)) {
            errors.rejectValue("preferredLanguageCode", "error.signup.preferredLanguageCode.invalid");
        }
    }
}
