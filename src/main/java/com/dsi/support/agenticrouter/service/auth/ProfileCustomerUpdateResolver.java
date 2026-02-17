package com.dsi.support.agenticrouter.service.auth;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.Country;
import com.dsi.support.agenticrouter.entity.CustomerTier;
import com.dsi.support.agenticrouter.entity.Language;
import com.dsi.support.agenticrouter.repository.CountryRepository;
import com.dsi.support.agenticrouter.repository.CustomerTierRepository;
import com.dsi.support.agenticrouter.repository.LanguageRepository;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;

import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ProfileCustomerUpdateResolver {

    private final CountryRepository countryRepository;
    private final CustomerTierRepository customerTierRepository;
    private final LanguageRepository languageRepository;

    public CustomerProfileUpdatePayload resolve(
        ApiDtos.ProfileUpdateRequest request
    ) throws BindException {
        BindingResult errors = BindValidation.bindingResult(
            request,
            "profileUpdateRequest"
        );

        String normalizedCompanyName = requiredTrimmedValue(
            request.companyName(),
            "companyName",
            "Company name is required.",
            errors
        );
        String normalizedPhoneNumber = requiredTrimmedValue(
            request.phoneNumber(),
            "phoneNumber",
            "Phone number is required.",
            errors
        );
        String normalizedAddress = requiredTrimmedValue(
            request.address(),
            "address",
            "Address is required.",
            errors
        );
        String normalizedCity = requiredTrimmedValue(
            request.city(),
            "city",
            "City is required.",
            errors
        );
        String normalizedCountryIso2 = requiredUppercaseValue(
            request.countryIso2(),
            "countryIso2",
            "Country is required.",
            errors
        );
        String normalizedTierCode = requiredUppercaseValue(
            request.customerTierCode(),
            "customerTierCode",
            "Tier is required.",
            errors
        );
        String normalizedLanguageCode = requiredTrimmedValue(
            request.preferredLanguageCode(),
            "preferredLanguageCode",
            "Preferred language is required.",
            errors
        );

        Country country = resolveRequiredReference(
            normalizedCountryIso2,
            "countryIso2",
            "Country is invalid.",
            countryRepository::findByIso2,
            errors
        );
        CustomerTier customerTier = resolveRequiredReference(
            normalizedTierCode,
            "customerTierCode",
            "Tier is invalid.",
            customerTierRepository::findByCode,
            errors
        );
        Language language = resolveRequiredReference(
            normalizedLanguageCode,
            "preferredLanguageCode",
            "Preferred language is invalid.",
            languageRepository::findByCode,
            errors
        );

        if (errors.hasErrors()) {
            throw BindValidation.exception(errors);
        }

        return new CustomerProfileUpdatePayload(
            normalizedCompanyName,
            normalizedPhoneNumber,
            normalizedAddress,
            normalizedCity,
            country,
            customerTier,
            language
        );
    }

    private <T> T resolveRequiredReference(
        String normalizedValue,
        String fieldName,
        String invalidMessage,
        Function<String, Optional<T>> resolver,
        BindingResult errors
    ) {
        if (StringUtils.isBlank(normalizedValue)) {
            return null;
        }

        Optional<T> resolved = resolver.apply(normalizedValue);
        if (resolved.isEmpty()) {
            BindValidation.rejectField(
                errors,
                fieldName,
                invalidMessage
            );
            return null;
        }

        return resolved.get();
    }

    private String requiredTrimmedValue(
        String value,
        String fieldName,
        String errorMessage,
        BindingResult errors
    ) {
        String normalizedValue = StringNormalizationUtils.trimToEmpty(value);
        if (StringUtils.isBlank(normalizedValue)) {
            BindValidation.rejectField(
                errors,
                fieldName,
                errorMessage
            );
            return null;
        }

        return normalizedValue;
    }

    private String requiredUppercaseValue(
        String value,
        String fieldName,
        String errorMessage,
        BindingResult errors
    ) {
        String normalized = requiredTrimmedValue(
            value,
            fieldName,
            errorMessage,
            errors
        );

        if (StringUtils.isBlank(normalized)) {
            return null;
        }

        return StringNormalizationUtils.upperTrimmedOrEmpty(normalized);
    }

    public record CustomerProfileUpdatePayload(
        String companyName,
        String phoneNumber,
        String address,
        String city,
        Country country,
        CustomerTier customerTier,
        Language preferredLanguage
    ) {
    }
}
