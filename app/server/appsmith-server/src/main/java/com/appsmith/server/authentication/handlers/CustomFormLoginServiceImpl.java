package com.appsmith.server.authentication.handlers;

import com.appsmith.server.authentication.handlers.ce.CustomFormLoginServiceCEImpl;
import com.appsmith.server.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// @Bonita remove @service, prefer use our customBonieServe see #CustomReactiveUserServiceBonitaImpl
// @Service
public class CustomFormLoginServiceImpl extends CustomFormLoginServiceCEImpl {

    public CustomFormLoginServiceImpl(UserRepository repository) {
        super(repository);
    }
}
