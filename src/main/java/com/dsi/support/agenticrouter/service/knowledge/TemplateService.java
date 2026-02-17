package com.dsi.support.agenticrouter.service.knowledge;

import com.dsi.support.agenticrouter.entity.ArticleTemplate;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.ArticleTemplateRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
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
        log.debug(
            "TemplateFill({}) Template(id:{}) Outcome(variableCount:{})",
            OperationalLogContext.PHASE_START,
            templateId,
            Objects.nonNull(variables) ? variables.size() : 0
        );

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

        log.debug(
            "TemplateFill({}) Template(id:{},name:{}) Outcome(contentLength:{})",
            OperationalLogContext.PHASE_COMPLETE,
            articleTemplate.getId(),
            articleTemplate.getName(),
            content.length()
        );

        return content;
    }

}
