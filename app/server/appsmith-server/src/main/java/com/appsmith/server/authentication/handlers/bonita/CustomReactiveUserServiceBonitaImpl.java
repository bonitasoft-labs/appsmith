package com.appsmith.server.authentication.handlers.bonita;

import com.appsmith.server.configurations.CommonConfig;
import com.appsmith.server.domains.LoginSource;
import com.appsmith.server.domains.User;
import com.appsmith.server.domains.UserState;
import com.appsmith.server.repositories.UserRepository;
import com.appsmith.server.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
// @Order(1)
public class CustomReactiveUserServiceBonitaImpl implements ReactiveUserDetailsService {

    private final UserRepository repository;
    private final UserService userService;

    @Autowired
    public CustomReactiveUserServiceBonitaImpl(UserRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        Mono<User> userSearch = repository.findByEmail(username);
        return userSearch
                .switchIfEmpty(repository.findFirstByEmailIgnoreCaseOrderByCreatedAtDesc(username))
                .switchIfEmpty(Mono.defer(() -> {
                    User newUser = new User();

                    // @Bonita: Create a user if it doesn't exist
                    newUser.setName(username);
                    newUser.setEmail(username);
                    newUser.setState(UserState.ACTIVATED);
                    newUser.setIsEnabled(true);
                    newUser.setSource(LoginSource.BONITA_DEV);
                    log.debug("### createUser with email: {}", username);
                    // @Bonita: Create an admin user if it doesn't exist
                    // With this function, the user is not added to admin mail in CommonConfig
                    //return userService.userCreate(newUser, true);
                    return userService.create(newUser);
                }))
                .flatMap(user -> {
                    if (Boolean.FALSE.equals(user.getIsEnabled())) {
                        user.setIsEnabled(true);
                        log.debug("### user setIsEnabled: {}", username);
                        return repository.save(user);
                    }
                    return Mono.just(user);
                });
    }
}
