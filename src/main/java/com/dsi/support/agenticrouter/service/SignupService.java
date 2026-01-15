package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.dto.SignupDto;
import com.dsi.support.agenticrouter.entity.*;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

@Service
@Transactional
@RequiredArgsConstructor
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
        model.addAttribute("countries", countryRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("tiers", customerTierRepository.findByActiveTrueOrderByDisplayNameAsc());
        model.addAttribute("languages", languageRepository.findAllByOrderByNameAsc());
    }

    public void signupCustomer(
        SignupDto signupDto
    ) {
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
    }
}
