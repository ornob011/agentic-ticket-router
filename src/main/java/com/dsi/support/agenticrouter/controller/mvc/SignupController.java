package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.dto.SignupDto;
import com.dsi.support.agenticrouter.service.SignupService;
import com.dsi.support.agenticrouter.util.Utils;
import com.dsi.support.agenticrouter.validator.SignupValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        model.addAttribute("signup", new SignupDto());

        signupService.loadDataForCustomerSignup(model);

        return "signup";
    }

    @PostMapping("/signup")
    public String handleSignup(
        @Valid @ModelAttribute("signup") SignupDto signupDto,
        BindingResult bindingResult,
        Model model,
        HttpServletRequest request
    ) {
        if (bindingResult.hasErrors()) {
            signupService.loadDataForCustomerSignup(model);

            model.addAttribute("signup", signupDto);

            return "signup";
        }

        signupService.signupCustomer(signupDto);

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "success.signup.created"
        );

        return "redirect:/login";
    }
}
