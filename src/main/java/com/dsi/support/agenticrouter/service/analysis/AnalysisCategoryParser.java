package com.dsi.support.agenticrouter.service.analysis;

import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class AnalysisCategoryParser {

    private static final Pattern TRAILING_CATEGORY_PATTERN = buildTrailingCategoryPattern();

    public TicketCategory parseFromModelResponse(
        String responseText
    ) {
        String normalizedResponse = StringNormalizationUtils.trimToEmpty(responseText);
        if (normalizedResponse.isEmpty()) {
            return TicketCategory.OTHER;
        }

        Matcher matcher = TRAILING_CATEGORY_PATTERN.matcher(normalizedResponse);
        if (!matcher.find()) {
            return TicketCategory.OTHER;
        }

        String categoryToken = StringNormalizationUtils.trimToEmpty(
            matcher.group(1)
        );

        return EnumUtils.getEnumIgnoreCase(
            TicketCategory.class,
            categoryToken,
            TicketCategory.OTHER
        );
    }

    private static Pattern buildTrailingCategoryPattern() {
        String categoryTokenAlternation = Arrays.stream(TicketCategory.values())
                                                .map(Enum::name)
                                                .sorted((first, second) -> Integer.compare(
                                                    second.length(),
                                                    first.length()
                                                ))
                                                .map(Pattern::quote)
                                                .collect(Collectors.joining("|"));

        return Pattern.compile(
            String.format("(?im)(%s)\\s*$", categoryTokenAlternation)
        );
    }
}
