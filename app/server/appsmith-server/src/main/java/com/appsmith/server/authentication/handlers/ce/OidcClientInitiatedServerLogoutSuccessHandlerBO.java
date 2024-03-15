package com.appsmith.server.authentication.handlers.ce;

import com.appsmith.server.domains.User;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class OidcClientInitiatedServerLogoutSuccessHandlerBO extends OidcClientInitiatedServerLogoutSuccessHandler {
    private final ReactiveClientRegistrationRepository clientRegistrationRepository;

    private final ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();

    private final RedirectServerLogoutSuccessHandler serverLogoutSuccessHandler =
            new RedirectServerLogoutSuccessHandler();

    private String postLogoutRedirectUri;

    /**
     * Constructs an {@link OidcClientInitiatedServerLogoutSuccessHandler} with the
     * provided parameters
     *
     * @param clientRegistrationRepository The
     *                                     {@link ReactiveClientRegistrationRepository} to use to derive the
     *                                     end_session_endpoint value
     */
    public OidcClientInitiatedServerLogoutSuccessHandlerBO(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {
        super(clientRegistrationRepository);
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
        // @formatter:off
        return Mono.just(authentication)
                .filter(OAuth2AuthenticationToken.class::isInstance)
                .filter((token) -> authentication.getPrincipal() instanceof OidcUser)
                .map(OAuth2AuthenticationToken.class::cast)
                .map(OAuth2AuthenticationToken::getAuthorizedClientRegistrationId)
                .flatMap(this.clientRegistrationRepository::findByRegistrationId)
                .flatMap((clientRegistration) -> {
                    URI endSessionEndpoint = endSessionEndpoint(clientRegistration);
                    if (endSessionEndpoint == null) {
                        return Mono.empty();
                    }
                    String idToken = idToken(authentication);
                    String postLogoutRedirectUri =
                            postLogoutRedirectUri(exchange.getExchange().getRequest(), clientRegistration);
                    return Mono.just(endpointUri(endSessionEndpoint, idToken, postLogoutRedirectUri));
                })
                .switchIfEmpty(this.serverLogoutSuccessHandler
                        .onLogoutSuccess(exchange, authentication)
                        .then(Mono.empty()))
                .flatMap((endpointUri) ->
                        this.redirectStrategy.sendRedirect(exchange.getExchange(), URI.create(endpointUri)));
        // @formatter:on
    }

    private URI endSessionEndpoint(ClientRegistration clientRegistration) {
        if (clientRegistration != null) {
            Object endSessionEndpoint = clientRegistration
                    .getProviderDetails()
                    .getConfigurationMetadata()
                    .get("end_session_endpoint");
            if (endSessionEndpoint != null) {
                return URI.create(endSessionEndpoint.toString());
            }
        }
        return null;
    }

    private String endpointUri(URI endSessionEndpoint, String idToken, String postLogoutRedirectUri) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(endSessionEndpoint);
        builder.queryParam("id_token_hint", idToken);
        if (postLogoutRedirectUri != null) {
            builder.queryParam("post_logout_redirect_uri", postLogoutRedirectUri);
        }
        return builder.encode(StandardCharsets.UTF_8).build().toUriString();
    }

    // @Bonita: Use the idTokenValue as string instead of oidcToken object
    private String idToken(Authentication authentication) {
        return ((User) authentication.getPrincipal()).getIdTokenValue();
    }

    private String postLogoutRedirectUri(ServerHttpRequest request, ClientRegistration clientRegistration) {
        if (this.postLogoutRedirectUri == null) {
            return null;
        }
        // @formatter:off
        UriComponents uriComponents = UriComponentsBuilder.fromUri(request.getURI())
                .replacePath(request.getPath().contextPath().value())
                .replaceQuery(null)
                .fragment(null)
                .build();

        Map<String, String> uriVariables = new HashMap<>();
        String scheme = uriComponents.getScheme();
        uriVariables.put("baseScheme", (scheme != null) ? scheme : "");
        uriVariables.put("baseUrl", uriComponents.toUriString());

        String host = uriComponents.getHost();
        uriVariables.put("baseHost", (host != null) ? host : "");

        String path = uriComponents.getPath();
        uriVariables.put("basePath", (path != null) ? path : "");

        int port = uriComponents.getPort();
        uriVariables.put("basePort", (port == -1) ? "" : ":" + port);

        uriVariables.put("registrationId", clientRegistration.getRegistrationId());

        return UriComponentsBuilder.fromUriString(this.postLogoutRedirectUri)
                .buildAndExpand(uriVariables)
                .toUriString();
        // @formatter:on
    }
}
