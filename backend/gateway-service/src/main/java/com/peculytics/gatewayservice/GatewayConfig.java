package com.peculytics.gatewayservice;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;

@Configuration
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> uploadServiceRoutes() {
        return route("upload-service")
                .route(path("/analyses/**"), http())
                .before(uri("lb://upload-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> apiServiceRoutes() {
        return route("api-service")
                .route(path("/analyses/*/transactions/**")
                        .or(path("/analyses/*/dashboard/**"))
                        .or(path("/rules/**")), http())
                .before(uri("lb://api-service"))
                .build();
    }

}
