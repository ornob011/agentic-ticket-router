package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.ArticleTemplate;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.ArticleTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.*;

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

    public ArticleTemplate findBestMatchingTemplate(
        TicketCategory category,
        TicketPriority priority,
        String subject
    ) {
        log.debug(
            "Finding template for category={}, priority={}, subject={}",
            category,
            priority,
            subject
        );

        List<List<ArticleTemplate>> templateCandidatesByStrategy = new ArrayList<>();

        if (Objects.nonNull(category) && Objects.nonNull(priority)) {
            log.debug("Trying exact category + priority match");
            templateCandidatesByStrategy.add(
                articleTemplateRepository.findByCategoryAndPriority(
                    category,
                    priority
                )
            );
        }

        if (Objects.nonNull(category)) {
            log.debug("No exact match found, trying category-only match");
            templateCandidatesByStrategy.add(
                articleTemplateRepository.findByCategoryOnly(
                    category
                )
            );
        }

        if (Objects.nonNull(priority)) {
            log.debug("No category match found, trying priority-only match");
            templateCandidatesByStrategy.add(
                articleTemplateRepository.findByPriorityOnly(
                    priority
                )
            );
        }

        log.debug("No specific match found, using global template");
        templateCandidatesByStrategy.add(
            articleTemplateRepository.findGlobalTemplates()
        );

        ArticleTemplate selectedTemplate = null;

        for (List<ArticleTemplate> templates : templateCandidatesByStrategy) {
            List<ArticleTemplate> safeTemplates = Objects.requireNonNullElse(
                templates,
                List.of()
            );

            if (safeTemplates.isEmpty()) {
                continue;
            }

            selectedTemplate = safeTemplates.getFirst();
            break;
        }

        if (Objects.isNull(selectedTemplate)) {
            log.warn("No templates found for selection criteria");
            return null;
        }

        log.info(
            "Selected template: {} (id={}) for category={}, priority={}",
            selectedTemplate.getName(),
            selectedTemplate.getId(),
            category,
            priority
        );

        return selectedTemplate;
    }
}
