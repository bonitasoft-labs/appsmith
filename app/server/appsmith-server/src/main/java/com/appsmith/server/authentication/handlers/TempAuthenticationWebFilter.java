package com.appsmith.server.authentication.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerResolver;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
public class TempAuthenticationWebFilter extends AuthenticationWebFilter {
    public TempAuthenticationWebFilter(ReactiveAuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    public TempAuthenticationWebFilter(
            ReactiveAuthenticationManagerResolver<ServerWebExchange> authenticationManagerResolver) {
        super(authenticationManagerResolver);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.debug("### TempAuthenticationWebFilter filter");
        return super.filter(exchange, chain);
    }
}
