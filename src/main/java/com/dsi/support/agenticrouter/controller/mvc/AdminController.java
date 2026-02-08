package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.dto.CreateStaffUserDto;
import com.dsi.support.agenticrouter.dto.PolicyConfigUpdateDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.ModelRegistry;
import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.enums.NavPage;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.service.ModelService;
import com.dsi.support.agenticrouter.service.PasswordHashService;
import com.dsi.support.agenticrouter.service.PolicyConfigService;
import com.dsi.support.agenticrouter.util.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final ModelService modelService;
    private final PolicyConfigService policyConfigService;
    private final PasswordHashService passwordHashService;

    private final MessageSource messageSource;

    @GetMapping("/admin/model-registry")
    @PreAuthorize("hasRole('ADMIN')")
    public String modelRegistry(Model model) {
        List<ModelRegistry> models = modelService.getAllModels();
        ModelRegistry activeModel = modelService.getActiveModel();

        model.addAttribute("models", models);
        model.addAttribute("activeModel", activeModel);
        model.addAttribute("currentPage", NavPage.ADMIN_MODEL_REGISTRY);

        return "admin/model-registry";
    }

    @PostMapping("/admin/model-registry/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public String activateModel(
        @RequestParam String modelTag,
        HttpServletRequest request
    ) {
        modelService.activateModel(modelTag, Utils.getLoggedInUserId());

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "model.activate.success"
        );

        return "redirect:/admin/model-registry";
    }

    @GetMapping("/admin/policy-config")
    @PreAuthorize("hasRole('ADMIN')")
    public String policyConfig(Model model) {
        List<PolicyConfig> policies = policyConfigService.getAllActivePolicies();

        model.addAttribute("policies", policies);
        model.addAttribute("currentPage", NavPage.ADMIN_POLICY_CONFIG);

        return "admin/policy-config";
    }

    @PostMapping("/admin/policy-config")
    @PreAuthorize("hasRole('ADMIN')")
    public String updatePolicyConfig(
        @Valid @ModelAttribute("policyConfigUpdateDto") PolicyConfigUpdateDto policyConfigUpdateDto,
        BindingResult bindingResult,
        Model model,
        HttpServletRequest request
    ) {
        if (bindingResult.hasErrors()) {
            List<PolicyConfig> policies = policyConfigService.getAllActivePolicies();

            model.addAttribute("policies", policies);
            model.addAttribute("policyConfigUpdateDto", policyConfigUpdateDto);
            model.addAttribute("currentPage", NavPage.ADMIN_POLICY_CONFIG);

            return "admin/policy-config";
        }

        policyConfigService.updatePolicy(
            policyConfigUpdateDto.getConfigKey(),
            policyConfigUpdateDto.getConfigValue()
        );

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "policy.config.update.success"
        );

        return "redirect:/admin/policy-config";
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Model model
    ) {
        List<AppUser> users = policyConfigService.getAllUsers();

        model.addAttribute("users", users);
        model.addAttribute("availableRoles", UserRole.values());
        model.addAttribute("currentPage", NavPage.ADMIN_USERS);

        return "admin/users";
    }

    @PostMapping("/admin/users/create-staff")
    @PreAuthorize("hasRole('ADMIN')")
    public String createStaffUser(
        @Valid @ModelAttribute("createStaffUserDto") CreateStaffUserDto createStaffUserDto,
        BindingResult bindingResult,
        Model model,
        HttpServletRequest request
    ) {
        if (bindingResult.hasErrors()) {
            List<AppUser> users = policyConfigService.getAllUsers();

            model.addAttribute("users", users);
            model.addAttribute("createStaffUserDto", createStaffUserDto);
            model.addAttribute("availableRoles", UserRole.values());
            model.addAttribute("currentPage", NavPage.ADMIN_USERS);

            return "admin/users";
        }

        String passwordHash = passwordHashService.getPasswordHash(
            createStaffUserDto.getPassword()
        );

        policyConfigService.createStaffUser(
            createStaffUserDto.getUsername(),
            createStaffUserDto.getEmail(),
            createStaffUserDto.getFullName(),
            createStaffUserDto.getRole(),
            passwordHash
        );

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "user.create.success"
        );

        return "redirect:/admin/users";
    }
}
