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
import com.dsi.support.agenticrouter.util.OperationalLogContext;
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
        log.info(
            "AdminModelRegistryView({}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            Utils.getLoggedInUserId()
        );

        List<ModelRegistry> models = modelService.getAllModels();
        ModelRegistry activeModel = modelService.getActiveModel();

        model.addAttribute("models", models);
        model.addAttribute("activeModel", activeModel);
        model.addAttribute("currentPage", NavPage.ADMIN_MODEL_REGISTRY);

        log.info(
            "AdminModelRegistryView({}) Actor(id:{}) Outcome(modelCount:{},activeModelId:{})",
            OperationalLogContext.PHASE_COMPLETE,
            Utils.getLoggedInUserId(),
            models.size(),
            activeModel.getId()
        );

        return "admin/model-registry";
    }

    @PostMapping("/admin/model-registry/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public String activateModel(
        @RequestParam String modelTag,
        HttpServletRequest request
    ) {
        log.info(
            "AdminModelActivate({}) Actor(id:{}) ModelRegistry(tag:{})",
            OperationalLogContext.PHASE_START,
            Utils.getLoggedInUserId(),
            modelTag
        );

        modelService.activateModel(modelTag, Utils.getLoggedInUserId());

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "model.activate.success"
        );

        log.info(
            "AdminModelActivate({}) Actor(id:{}) ModelRegistry(tag:{})",
            OperationalLogContext.PHASE_COMPLETE,
            Utils.getLoggedInUserId(),
            modelTag
        );

        return "redirect:/admin/model-registry";
    }

    @GetMapping("/admin/policy-config")
    @PreAuthorize("hasRole('ADMIN')")
    public String policyConfig(Model model) {
        log.info(
            "AdminPolicyView({}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            Utils.getLoggedInUserId()
        );

        List<PolicyConfig> policies = policyConfigService.getAllActivePolicies();

        model.addAttribute("policies", policies);
        model.addAttribute("currentPage", NavPage.ADMIN_POLICY_CONFIG);

        log.info(
            "AdminPolicyView({}) Actor(id:{}) Outcome(policyCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            Utils.getLoggedInUserId(),
            policies.size()
        );

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
        log.info(
            "AdminPolicyUpdate({}) Actor(id:{}) PolicyConfig(key:{},value:{})",
            OperationalLogContext.PHASE_START,
            Utils.getLoggedInUserId(),
            policyConfigUpdateDto.getConfigKey(),
            policyConfigUpdateDto.getConfigValue()
        );

        if (bindingResult.hasErrors()) {
            log.warn(
                "AdminPolicyUpdate({}) Actor(id:{}) Outcome(validationErrors:{})",
                OperationalLogContext.PHASE_FAIL,
                Utils.getLoggedInUserId(),
                bindingResult.getErrorCount()
            );
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

        log.info(
            "AdminPolicyUpdate({}) Actor(id:{}) PolicyConfig(key:{})",
            OperationalLogContext.PHASE_COMPLETE,
            Utils.getLoggedInUserId(),
            policyConfigUpdateDto.getConfigKey()
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
        log.info(
            "AdminUserList({}) Actor(id:{}) Outcome(page:{},size:{})",
            OperationalLogContext.PHASE_START,
            Utils.getLoggedInUserId(),
            page,
            size
        );

        List<AppUser> users = policyConfigService.getAllUsers();

        model.addAttribute("users", users);
        model.addAttribute("availableRoles", UserRole.values());
        model.addAttribute("currentPage", NavPage.ADMIN_USERS);

        log.info(
            "AdminUserList({}) Actor(id:{}) Outcome(userCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            Utils.getLoggedInUserId(),
            users.size()
        );

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
        log.info(
            "AdminStaffUserCreate({}) Actor(id:{}) AppUser(username:{},email:{},role:{})",
            OperationalLogContext.PHASE_START,
            Utils.getLoggedInUserId(),
            createStaffUserDto.getUsername(),
            createStaffUserDto.getEmail(),
            createStaffUserDto.getRole()
        );

        if (bindingResult.hasErrors()) {
            log.warn(
                "AdminStaffUserCreate({}) Actor(id:{}) Outcome(validationErrors:{})",
                OperationalLogContext.PHASE_FAIL,
                Utils.getLoggedInUserId(),
                bindingResult.getErrorCount()
            );

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

        log.info(
            "AdminStaffUserCreate({}) Actor(id:{}) AppUser(username:{},role:{})",
            OperationalLogContext.PHASE_COMPLETE,
            Utils.getLoggedInUserId(),
            createStaffUserDto.getUsername(),
            createStaffUserDto.getRole()
        );

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "user.create.success"
        );

        return "redirect:/admin/users";
    }
}
