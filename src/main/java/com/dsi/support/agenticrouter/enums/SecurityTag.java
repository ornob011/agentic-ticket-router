package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.List;
import java.util.stream.Stream;

@Getter
public enum SecurityTag {

    THREAT("THREAT"),
    PII_RISK("PII_RISK"),
    SECURITY_BREACH("SECURITY_BREACH"),
    HACK("HACK"),
    BREACH("BREACH"),
    VULNERABILITY("VULNERABILITY"),
    MALWARE("MALWARE"),
    CYBER_ATTACK("CYBER_ATTACK"),
    EXPLOIT("EXPLOIT"),


    ;

    private final String code;

    SecurityTag(
        String code
    ) {
        this.code = code;
    }

    public static List<String> getDangerousTags() {
        return Stream.of(values())
                     .map(tag -> tag.code)
                     .toList();
    }
}
