package com.appsmith.server.configurations.bonita;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import javax.security.auth.Subject;
import java.util.Collection;

public class BonitaDevAuthentificationToken extends AbstractAuthenticationToken {

    private final Object principal;

    public BonitaDevAuthentificationToken(Object principal) {
        super(null);
        this.principal = principal;
        setAuthenticated(false);
    }

    public BonitaDevAuthentificationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        super.setAuthenticated(true);
    }

    public static BonitaDevAuthentificationToken authenticated(Object principal) {
        BonitaDevAuthentificationToken auth = new BonitaDevAuthentificationToken(principal);
        auth.setAuthenticated(true);
        return auth;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public boolean implies(Subject subject) {
        return super.implies(subject);
    }
}
