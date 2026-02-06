package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.ArticleTemplate;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.ArticleTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private static final String OPEN = "{{";
    private static final String CLOSE = "}}";

    private final ArticleTemplateRepository articleTemplateRepository;

    public String fillTemplate(
        Long templateId,
        Map<String, String> variables
    ) {
        ArticleTemplate articleTemplate = articleTemplateRepository.findById(templateId)
                                                                   .orElseThrow(
                                                                       DataNotFoundException.supplier(
                                                                           ArticleTemplate.class,
                                                                           templateId
                                                                       )
                                                                   );

        Map<String, String> safeVariables = Objects.requireNonNullElse(
            variables,
            Map.of()
        );

        List<String> allowedVariables = Optional.ofNullable(articleTemplate.getVariables())
                                                .orElse(List.of());

        String content = StringUtils.defaultString(articleTemplate.getTemplateContent());

        for (String variable : allowedVariables) {
            String placeholder = OPEN + variable + CLOSE;

            String replacement = Optional.ofNullable(safeVariables.get(variable))
                                         .map(StringUtils::trimToNull)
                                         .orElse(placeholder);

            content = Strings.CS.replace(
                content,
                placeholder,
                replacement
            );
        }

        return content;
    }
}
