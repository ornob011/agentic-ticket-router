package com.dsi.support.agenticrouter.configuration;

import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private static final String APP_RESOURCE_PATTERN = "/app/**";
    private static final String APP_RESOURCE_LOCATION = "classpath:/static/app/";
    private static final String APP_INDEX_FILE = "index.html";

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Override
    public void addViewControllers(
        ViewControllerRegistry viewControllerRegistry
    ) {
        String appIndexForward = AppRoutePolicy.appIndexForwardViewName();

        viewControllerRegistry.addViewController(AppRoutePolicy.WebRoute.ROOT.path())
                              .setViewName("redirect:" + AppRoutePolicy.WebRoute.DASHBOARD.path());

        AppRoutePolicy.spaEntryRoutes().forEach(
            route -> viewControllerRegistry.addViewController(
                route.path()
            ).setViewName(appIndexForward)
        );

        AppRoutePolicy.spaScopeRoutes().forEach(
            route -> viewControllerRegistry.addViewController(
                route.path()
            ).setViewName(appIndexForward)
        );
    }

    @Override
    public void addResourceHandlers(
        ResourceHandlerRegistry resourceHandlerRegistry
    ) {
        resourceHandlerRegistry.addResourceHandler(APP_RESOURCE_PATTERN)
                               .addResourceLocations(APP_RESOURCE_LOCATION)
                               .resourceChain(true)
                               .addResolver(new SpaFallbackPathResourceResolver());
    }

    private static final class SpaFallbackPathResourceResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(
            @NonNull String resourcePath,
            Resource location
        ) throws IOException {
            Resource requestedResource = location.createRelative(resourcePath);

            if (requestedResource.exists() && requestedResource.isReadable()) {
                return requestedResource;
            }

            return location.createRelative(APP_INDEX_FILE);
        }
    }
}
