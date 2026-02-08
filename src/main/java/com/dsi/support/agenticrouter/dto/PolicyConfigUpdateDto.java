package com.dsi.support.agenticrouter.dto;

import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyConfigUpdateDto {

    @NotNull(message = "{policy.config.key.required}")
    private PolicyConfigKey configKey;

    @NotNull(message = "{policy.config.value.required}")
    @DecimalMin(value = "0", message = "{policy.config.value.min}")
    private BigDecimal configValue;
}
