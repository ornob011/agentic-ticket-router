package com.dsi.support.agenticrouter.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Override
    public void addViewControllers(
        ViewControllerRegistry viewControllerRegistry
    ) {
        viewControllerRegistry.addViewController(AppRoutePolicy.WebRoute.ROOT.path())
                              .setViewName("redirect:" + AppRoutePolicy.WebRoute.DASHBOARD.path());
    }
}
