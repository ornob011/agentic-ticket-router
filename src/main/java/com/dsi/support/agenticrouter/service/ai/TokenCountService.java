package com.dsi.support.agenticrouter.service.ai;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class TokenCountService {

    private final Encoding encoding;

    public TokenCountService() {
        EncodingRegistry encodingRegistry = Encodings.newLazyEncodingRegistry();
        this.encoding = encodingRegistry.getEncoding(
            EncodingType.CL100K_BASE
        );
    }

    public int countTokens(
        String value
    ) {
        String normalizedValue = StringUtils.trimToNull(value);

        if (StringUtils.isBlank(normalizedValue)) {
            return 0;
        }

        return encoding.countTokens(normalizedValue);
    }
}
