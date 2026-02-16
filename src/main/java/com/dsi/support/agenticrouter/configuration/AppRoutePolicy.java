package com.dsi.support.agenticrouter.configuration;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public final class AppRoutePolicy {

    private static final EnumSet<WebRoute> PUBLIC_WEB_ROUTES = EnumSet.of(
        WebRoute.ROOT,
        WebRoute.APP,
        WebRoute.APP_SLASH,
        WebRoute.APP_INDEX,
        WebRoute.LOGIN,
        WebRoute.SIGNUP,
        WebRoute.FAVICON,
        WebRoute.ASSETS,
        WebRoute.APP_ASSETS
    );
    private static final EnumSet<WebRoute> SPA_ENTRY_ROUTES = EnumSet.of(
        WebRoute.APP,
        WebRoute.APP_SLASH,
        WebRoute.LOGIN,
        WebRoute.SIGNUP,
        WebRoute.DASHBOARD,
        WebRoute.SETTINGS
    );
    private static final EnumSet<ScopeRoute> SPA_SCOPE_ROUTES = EnumSet.of(
        ScopeRoute.TICKETS,
        ScopeRoute.AGENT,
        ScopeRoute.SUPERVISOR,
        ScopeRoute.ADMIN
    );

    private AppRoutePolicy() {
    }

    public static String[] publicAppEndpoints() {
        return PUBLIC_WEB_ROUTES.stream()
                                .map(WebRoute::path)
                                .toArray(String[]::new);
    }

    public static String[] publicApiEndpoints() {
        return Arrays.stream(ApiRoute.values())
                     .map(ApiRoute::path)
                     .toArray(String[]::new);
    }

    public static Set<WebRoute> spaEntryRoutes() {
        return EnumSet.copyOf(SPA_ENTRY_ROUTES);
    }

    public static Set<ScopeRoute> spaScopeRoutes() {
        return EnumSet.copyOf(SPA_SCOPE_ROUTES);
    }

    public static String appIndexForwardViewName() {
        return "forward:" + WebRoute.APP_INDEX.path();
    }

    public enum WebRoute implements RoutePath {
        ROOT("/"),
        APP("/app"),
        APP_SLASH("/app/"),
        APP_INDEX("/app/index.html"),
        LOGIN("/app/login"),
        SIGNUP("/app/signup"),
        DASHBOARD("/app/dashboard"),
        SETTINGS("/app/settings"),
        FAVICON("/favicon.ico"),
        ASSETS("/assets/**"),
        APP_ASSETS("/app/assets/**");

        private final String path;

        WebRoute(String path) {
            this.path = path;
        }

        public String path() {
            return path;
        }
    }

    public enum ApiRoute implements RoutePath {
        AUTH_LOGIN("/api/v1/auth/login"),
        AUTH_SIGNUP("/api/v1/auth/signup"),
        AUTH_SIGNUP_OPTIONS("/api/v1/auth/signup-options"),
        DEV_API("/api/dev/**");

        private final String path;

        ApiRoute(String path) {
            this.path = path;
        }

        public String path() {
            return path;
        }
    }

    public enum ScopeRoute implements RoutePath {
        API("/api/**"),
        TICKETS("/app/tickets/**"),
        AGENT("/app/agent/**"),
        SUPERVISOR("/app/supervisor/**"),
        ADMIN("/app/admin/**");

        private final String path;

        ScopeRoute(String path) {
            this.path = path;
        }

        public String path() {
            return path;
        }
    }

    public interface RoutePath {
        String path();
    }
}
