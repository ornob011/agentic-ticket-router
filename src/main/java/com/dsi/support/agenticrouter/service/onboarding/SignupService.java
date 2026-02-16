package com.dsi.support.agenticrouter.service.onboarding;

import com.dsi.support.agenticrouter.dto.SignupDto;
import com.dsi.support.agenticrouter.entity.*;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.*;
import com.dsi.support.agenticrouter.service.auth.PasswordHashService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SignupService {

    private final AppUserRepository appUserRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CountryRepository countryRepository;
    private final CustomerTierRepository customerTierRepository;
    private final LanguageRepository languageRepository;
    private final PasswordHashService passwordHashService;

    public void loadDataForCustomerSignup(
        Model model
    ) {
        List<Country> countries = countryRepository.findByActiveTrueOrderByNameAsc();

        List<CustomerTier> customerTiers = customerTierRepository.findByActiveTrueOrderByDisplayNameAsc();

        List<Language> languages = languageRepository.findAllByOrderByNameAsc();

        model.addAttribute("countries", countries);
        model.addAttribute("tiers", customerTiers);
        model.addAttribute("languages", languages);

        log.debug(
            "SignupReferenceDataLoad({}) Outcome(countryCount:{},tierCount:{},languageCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            countries.size(),
            customerTiers.size(),
            languages.size()
        );
    }

    public void signupCustomer(
        SignupDto signupDto
    ) {
        log.info(
            "SignupCustomer({}) AppUser(usernameLength:{},emailLength:{})",
            OperationalLogContext.PHASE_START,
            StringUtils.length(StringNormalizationUtils.trimToNull(signupDto.getUsername())),
            StringUtils.length(StringNormalizationUtils.trimToNull(signupDto.getEmail()))
        );

        String normalizedUsername = StringNormalizationUtils.lowerTrimmedOrNull(signupDto.getUsername());

        String normalizedEmail = StringNormalizationUtils.lowerTrimmedOrNull(signupDto.getEmail());

        String normalizedFullName = StringNormalizationUtils.trimToNull(signupDto.getFullName());

        String normalizedCompanyName = StringNormalizationUtils.trimToNull(signupDto.getCompanyName());

        String normalizedPhoneNumber = StringNormalizationUtils.trimToNull(signupDto.getPhoneNumber());

        String normalizedAddress = StringNormalizationUtils.trimToNull(signupDto.getAddress());

        String normalizedCity = StringNormalizationUtils.trimToNull(signupDto.getCity());

        String normalizedCountryIso2 = StringNormalizationUtils.upperTrimmedOrNull(signupDto.getCountryIso2());

        String normalizedTierCode = StringNormalizationUtils.trimToNull(signupDto.getCustomerTierCode());

        String normalizedLanguageCode = StringNormalizationUtils.trimToEmpty(signupDto.getPreferredLanguageCode());

        Country selectedCountry = countryRepository.findByIso2(normalizedCountryIso2)
                                                   .orElseThrow(
                                                       DataNotFoundException.supplier(
                                                           Country.class,
                                                           normalizedCountryIso2
                                                       )
                                                   );

        CustomerTier selectedTier = customerTierRepository.findByCode(normalizedTierCode)
                                                          .orElseThrow(
                                                              DataNotFoundException.supplier(
                                                                  CustomerTier.class,
                                                                  normalizedTierCode
                                                              )
                                                          );

        Language selectedLanguage = languageRepository.findByCode(normalizedLanguageCode)
                                                      .orElseThrow(
                                                          DataNotFoundException.supplier(
                                                              Language.class,
                                                              normalizedLanguageCode
                                                          )
                                                      );

        String passwordHash = passwordHashService.getPasswordHash(
            signupDto.getPassword()
        );

        AppUser newCustomerUser = AppUser.builder()
                                         .username(normalizedUsername)
                                         .email(normalizedEmail)
                                         .passwordHash(passwordHash)
                                         .fullName(normalizedFullName)
                                         .role(UserRole.CUSTOMER)
                                         .active(true)
                                         .emailVerified(false)
                                         .build();

        CustomerProfile newCustomerProfile = CustomerProfile.builder()
                                                            .user(newCustomerUser)
                                                            .companyName(normalizedCompanyName)
                                                            .phoneNumber(normalizedPhoneNumber)
                                                            .address(normalizedAddress)
                                                            .city(normalizedCity)
                                                            .country(selectedCountry)
                                                            .customerTier(selectedTier)
                                                            .preferredLanguage(selectedLanguage)
                                                            .notificationsEnabled(true)
                                                            .build();

        appUserRepository.save(newCustomerUser);
        customerProfileRepository.save(newCustomerProfile);

        log.info(
            "SignupCustomer({}) AppUser(id:{},username:{},role:{},active:{}) CustomerProfile(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            newCustomerUser.getId(),
            newCustomerUser.getUsername(),
            newCustomerUser.getRole(),
            newCustomerUser.isActive(),
            newCustomerProfile.getId()
        );
    }
}
