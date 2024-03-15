package com.appsmith.server.configurations.bonita;

import com.appsmith.server.authentication.handlers.bonita.CustomReactiveUserServiceBonitaImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ReactiveAuthenticationManagerBonitaImpl implements ReactiveAuthenticationManager {
    private final CustomReactiveUserServiceBonitaImpl customReactiveUserServiceBonita;

    @Autowired
    public ReactiveAuthenticationManagerBonitaImpl(
            CustomReactiveUserServiceBonitaImpl customReactiveUserServiceBonita) {
        this.customReactiveUserServiceBonita = customReactiveUserServiceBonita;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        log.debug("ReactiveAuthenticationManagerBonitaImpl: authenticate");
        // BonitaDevAuthenticationToken is a custom token.
        if (authentication instanceof BonitaDevAuthenticationToken) {
            String email = (String) authentication.getPrincipal();
            log.debug("User: {}", email);
            // @Bonita: Create user if doesn't exist
            return customReactiveUserServiceBonita
                    .findByUsername(email)
                    .flatMap(user -> Mono.just(BonitaDevAuthenticationToken.authenticated(user)));
        }
        return Mono.just(authentication);
    }
}
