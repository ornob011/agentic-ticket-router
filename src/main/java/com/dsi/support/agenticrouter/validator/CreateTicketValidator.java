package com.dsi.support.agenticrouter.validator;

import com.dsi.support.agenticrouter.dto.CreateTicketDto;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class CreateTicketValidator implements Validator {

    private static final int SUBJECT_MIN = 5;
    private static final int SUBJECT_MAX = 150;

    private static final int CONTENT_MIN = 10;
    private static final int CONTENT_MAX = 10_000;

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return CreateTicketDto.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(
        @NonNull Object target,
        @NonNull Errors errors
    ) {
        CreateTicketDto createTicketDto = (CreateTicketDto) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "subject", "ticket.subject.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "content", "ticket.content.required");

        if (!errors.hasFieldErrors("subject") && StringUtils.hasText(createTicketDto.getSubject())) {
            String subject = createTicketDto.getSubject();

            int subjectLength = subject.length();

            if (subjectLength < SUBJECT_MIN || subjectLength > SUBJECT_MAX) {
                errors.rejectValue("subject", "ticket.subject.length");
            }
        }

        if (!errors.hasFieldErrors("content") && StringUtils.hasText(createTicketDto.getContent())) {
            String content = createTicketDto.getContent();
            int contentLength = content.length();

            if (contentLength < CONTENT_MIN || contentLength > CONTENT_MAX) {
                errors.rejectValue("content", "ticket.content.length");
            }
        }
    }
}
