package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.dto.SignupDto;
import com.dsi.support.agenticrouter.entity.*;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            "SignupReferenceDataLoad(complete) Outcome(countryCount:{},tierCount:{},languageCount:{})",
            countries.size(),
            customerTiers.size(),
            languages.size()
        );
    }

    public void signupCustomer(
        SignupDto signupDto
    ) {
        log.info(
            "SignupCustomer(start) AppUser(usernameLength:{},emailLength:{})",
            signupDto.getUsername() != null ? signupDto.getUsername().trim().length() : 0,
            signupDto.getEmail() != null ? signupDto.getEmail().trim().length() : 0
        );

        String normalizedUsername = signupDto.getUsername()
                                             .trim()
                                             .toLowerCase();

        String normalizedEmail = signupDto.getEmail()
                                          .trim()
                                          .toLowerCase();

        String normalizedFullName = signupDto.getFullName()
                                             .trim();

        String normalizedCompanyName = signupDto.getCompanyName()
                                                .trim();

        String normalizedPhoneNumber = signupDto.getPhoneNumber()
                                                .trim();

        String normalizedAddress = signupDto.getAddress()
                                            .trim();

        String normalizedCity = signupDto.getCity()
                                         .trim();

        String normalizedCountryIso2 = signupDto.getCountryIso2()
                                                .trim();

        String normalizedTierCode = signupDto.getCustomerTierCode()
                                             .trim();

        String normalizedLanguageCode = signupDto.getPreferredLanguageCode()
                                                 .trim();

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
            "SignupCustomer(complete) AppUser(id:{},username:{},role:{},active:{}) CustomerProfile(id:{})",
            newCustomerUser.getId(),
            newCustomerUser.getUsername(),
            newCustomerUser.getRole(),
            newCustomerUser.isActive(),
            newCustomerProfile.getId()
        );
    }
}
