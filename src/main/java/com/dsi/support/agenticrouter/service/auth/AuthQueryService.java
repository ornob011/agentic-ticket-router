package com.dsi.support.agenticrouter.service.auth;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.repository.CountryRepository;
import com.dsi.support.agenticrouter.repository.CustomerTierRepository;
import com.dsi.support.agenticrouter.repository.LanguageRepository;
import com.dsi.support.agenticrouter.service.common.UserMeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthQueryService {

    private final CountryRepository countryRepository;
    private final CustomerTierRepository customerTierRepository;
    private final LanguageRepository languageRepository;
    private final UserMeMapper userMeMapper;

    @Transactional(readOnly = true)
    public ApiDtos.SignupOptionsResponse signupOptions() {
        List<ApiDtos.LookupOption> countries = countryRepository.findByActiveTrueOrderByNameAsc()
                                                                .stream()
                                                                .map(country -> ApiDtos.LookupOption.builder()
                                                                                                    .code(country.getIso2())
                                                                                                    .name(country.getName())
                                                                                                    .build())
                                                                .toList();

        List<ApiDtos.LookupOption> tiers = customerTierRepository.findByActiveTrueOrderByDisplayNameAsc()
                                                                 .stream()
                                                                 .map(tier -> ApiDtos.LookupOption.builder()
                                                                                                 .code(tier.getCode())
                                                                                                 .name(tier.getDisplayName())
                                                                                                 .build())
                                                                 .toList();

        List<ApiDtos.LookupOption> languages = languageRepository.findAllByOrderByNameAsc()
                                                                 .stream()
                                                                 .map(language -> ApiDtos.LookupOption.builder()
                                                                                                     .code(language.getCode())
                                                                                                     .name(language.getName())
                                                                                                     .build())
                                                                 .toList();

        return ApiDtos.SignupOptionsResponse.builder()
                                            .countries(countries)
                                            .tiers(tiers)
                                            .languages(languages)
                                            .build();
    }

    public ApiDtos.UserMe toUserMe(
        AppUser appUser
    ) {
        return userMeMapper.toUserMe(appUser);
    }
}
