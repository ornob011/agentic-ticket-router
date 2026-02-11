package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.dto.SignupDto;
import com.dsi.support.agenticrouter.service.onboarding.SignupService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import com.dsi.support.agenticrouter.validator.SignupValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SignupController {

    private final SignupService signupService;
    private final SignupValidator signupValidator;
    private final MessageSource messageSource;

    @InitBinder("signup")
    public void bindValidator(WebDataBinder binder) {
        binder.addValidators(signupValidator);
    }

    @GetMapping("/signup")
    public String showSignup(Model model) {
        log.info(
            "SignupView({})",
            OperationalLogContext.PHASE_START
        );

        model.addAttribute("signup", new SignupDto());

        signupService.loadDataForCustomerSignup(model);

        log.info(
            "SignupView({}) Outcome(view:{})",
            OperationalLogContext.PHASE_COMPLETE,
            "signup"
        );

        return "signup";
    }

    @PostMapping("/signup")
    public String handleSignup(
        @Valid @ModelAttribute("signup") SignupDto signupDto,
        BindingResult bindingResult,
        Model model,
        HttpServletRequest request
    ) {
        log.info(
            "SignupSubmit({}) Outcome(usernameLength:{},emailLength:{})",
            OperationalLogContext.PHASE_START,
            StringUtils.length(StringUtils.trimToNull(signupDto.getUsername())),
            StringUtils.length(StringUtils.trimToNull(signupDto.getEmail()))
        );

        if (bindingResult.hasErrors()) {
            log.warn(
                "SignupSubmit({}) Outcome(validationErrors:{})",
                OperationalLogContext.PHASE_FAIL,
                bindingResult.getErrorCount()
            );

            signupService.loadDataForCustomerSignup(model);

            model.addAttribute("signup", signupDto);

            return "signup";
        }

        signupService.signupCustomer(signupDto);

        log.info(
            "SignupSubmit({}) Outcome(username:{})",
            OperationalLogContext.PHASE_COMPLETE,
            signupDto.getUsername()
        );

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "success.signup.created"
        );

        return "redirect:/login";
    }
}
