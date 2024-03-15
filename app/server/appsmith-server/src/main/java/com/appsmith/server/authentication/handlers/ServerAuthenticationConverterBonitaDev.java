package com.appsmith.server.authentication.handlers;

import com.appsmith.server.configurations.bonita.BonitaDevAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class ServerAuthenticationConverterBonitaDev implements ServerAuthenticationConverter {

    private final String email;

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        log.debug("### ServerAuthenticationConverterBonitaDev - convert {}", email);
        return Mono.just(new BonitaDevAuthenticationToken(email));
    }
}
