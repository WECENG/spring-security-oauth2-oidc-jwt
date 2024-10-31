package com.weceng.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@SpringBootTest
@Slf4j
class ClientRegistrarTest {

    private final static String AUTH_SERVER_BASE_URL = "http://127.0.0.1:9000";
    private final static String PUBLIC_CLIENT_BASE_URL = "http://oauth2publicclient.com:8005";
    private final static String SECRET_CLIENT_BASE_URL = "http://oauth2secretclient.com:8006";

    @Autowired
    private WebClient webClient;

    @Data
    private static class RegistrarToken {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("scope")
        private String scope;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private Long expires;
    }

    private String initialAccessToken() {
        String base64_encoded_credentials = Base64.getEncoder().encodeToString("registrar-client:registrar-client-secret".getBytes(StandardCharsets.UTF_8));
        RegistrarToken registrarToken = webClient.post().uri(AUTH_SERVER_BASE_URL + "/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic %s".formatted(base64_encoded_credentials))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials").with("scope", "client.create"))
                .retrieve()
                .bodyToMono(RegistrarToken.class)
                .block();
        assert registrarToken != null;
        return registrarToken.getAccessToken();
    }

    @Test
    void examplePublicClientRegistration() {

        ClientRegistrar registrar = new ClientRegistrar(webClient);


        ClientRegistrar.ClientRegistrationRequest clientRegistrationRequest = ClientRegistrar.ClientRegistrationRequest.builder()
                .clientName("public-client")
                .grantTypes(List.of(AuthorizationGrantType.AUTHORIZATION_CODE.getValue(), AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()))
                .redirectUris(List.of(PUBLIC_CLIENT_BASE_URL, PUBLIC_CLIENT_BASE_URL + "/login/oauth2/code/public-client"))
                //公共应用public client,启用PKCE，需要设置此值。但是已不支持动态注册public client.
                // issue: https://github.com/spring-projects/spring-authorization-server/issues/625
                .clientAuthenticationMethods(List.of("none"))
                //server中的转换器默认设置为true
                .requireProofKey(true)
                .requireAuthorizationConsent(false)
                .userUri(PUBLIC_CLIENT_BASE_URL + "/user")
                .contacts(List.of("contact-1", "contact-2"))
                .scope("read write openid").build();

        ClientRegistrar.ClientRegistrationResponse clientRegistrationResponse = registrar.registerClient(initialAccessToken(), clientRegistrationRequest);

        log.info("Public Client 注册的client_secret原文：{}", clientRegistrationResponse.getClientSecret());
        assert (clientRegistrationResponse.getClientName().contentEquals("public-client"));
        assert (!Objects.isNull(clientRegistrationResponse.getClientSecret()));
        assert (clientRegistrationResponse.getScope().contentEquals("read openid write"));
        assert (clientRegistrationResponse.getGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue()));
        assert (clientRegistrationResponse.getGrantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()));
        assert (clientRegistrationResponse.getRedirectUris().contains(PUBLIC_CLIENT_BASE_URL));
        assert (clientRegistrationResponse.getRedirectUris().contains(PUBLIC_CLIENT_BASE_URL + "/login/oauth2/code/public-client"));
        assert (!clientRegistrationResponse.getRegistrationAccessToken().isEmpty());
        assert (!clientRegistrationResponse.getRegistrationClientUri().isEmpty());
        assert (clientRegistrationResponse.getRequireProofKey().equals(true));
        assert (clientRegistrationResponse.getRequireAuthorizationConsent().equals(false));
        assert (clientRegistrationResponse.getUserUri().contentEquals(PUBLIC_CLIENT_BASE_URL + "/user"));
        assert (clientRegistrationResponse.getContacts().size() == 2);
        assert (clientRegistrationResponse.getContacts().contains("contact-1"));
        assert (clientRegistrationResponse.getContacts().contains("contact-2"));

        String registrationAccessToken = clientRegistrationResponse.getRegistrationAccessToken();
        String registrationClientUri = clientRegistrationResponse.getRegistrationClientUri();

        ClientRegistrar.ClientRegistrationResponse retrievedClient = registrar.retrieveClient(registrationAccessToken, registrationClientUri);

        assert (retrievedClient.getClientName().contentEquals("public-client"));
        assert (!Objects.isNull(retrievedClient.getClientId()));
        assert (!Objects.isNull(retrievedClient.getClientSecret()));
        assert (retrievedClient.getScope().contentEquals("read openid write"));
        assert (retrievedClient.getGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue()));
        assert (retrievedClient.getGrantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()));
        assert (retrievedClient.getRedirectUris().contains(PUBLIC_CLIENT_BASE_URL));
        assert (retrievedClient.getRedirectUris().contains(PUBLIC_CLIENT_BASE_URL + "/login/oauth2/code/public-client"));
        assert (retrievedClient.getUserUri().contentEquals(PUBLIC_CLIENT_BASE_URL + "/user"));
        assert (retrievedClient.getContacts().size() == 2);
        assert (retrievedClient.getContacts().contains("contact-1"));
        assert (retrievedClient.getContacts().contains("contact-2"));
        assert (Objects.isNull(retrievedClient.getRegistrationAccessToken()));
        assert (!retrievedClient.getRegistrationClientUri().isEmpty());

        log.info("Public Client 注册信息：{}", retrievedClient);
    }

    @Test
    void exampleSecretClientRegistration() {

        ClientRegistrar registrar = new ClientRegistrar(webClient);


        ClientRegistrar.ClientRegistrationRequest clientRegistrationRequest = ClientRegistrar.ClientRegistrationRequest.builder()
                .clientName("secret-client")
                //指定
                .clientId("secret-client")
                //指定
                .clientSecret("secret-client-secret")
                .grantTypes(List.of(AuthorizationGrantType.AUTHORIZATION_CODE.getValue(), AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()))
                .redirectUris(List.of(SECRET_CLIENT_BASE_URL, SECRET_CLIENT_BASE_URL + "/login/oauth2/code/secret-client"))
                //公共应用public client,启用PKCE，需要设置此值。但是已不支持动态注册public client.
                // issue: https://github.com/spring-projects/spring-authorization-server/issues/625
                .clientAuthenticationMethods(List.of("client_secret_basic"))
                .requireProofKey(false)
                .requireAuthorizationConsent(false)
                .userUri(SECRET_CLIENT_BASE_URL + "/user")
                .contacts(List.of("contact-1", "contact-2"))
                .scope("read write openid").build();

        ClientRegistrar.ClientRegistrationResponse clientRegistrationResponse = registrar.registerClient(initialAccessToken(), clientRegistrationRequest);

        log.info("Secret Client 注册的client_secret原文：{}", clientRegistrationResponse.getClientSecret());
        assert (clientRegistrationResponse.getClientName().contentEquals("secret-client"));
        assert (!Objects.isNull(clientRegistrationResponse.getClientSecret()));
        assert (clientRegistrationResponse.getScope().contentEquals("read openid write"));
        assert (clientRegistrationResponse.getGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue()));
        assert (clientRegistrationResponse.getGrantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()));
        assert (clientRegistrationResponse.getRedirectUris().contains(SECRET_CLIENT_BASE_URL));
        assert (clientRegistrationResponse.getRedirectUris().contains(SECRET_CLIENT_BASE_URL + "/login/oauth2/code/secret-client"));
        assert (!clientRegistrationResponse.getRegistrationAccessToken().isEmpty());
        assert (!clientRegistrationResponse.getRegistrationClientUri().isEmpty());
        assert (clientRegistrationResponse.getRequireProofKey().equals(false));
        assert (clientRegistrationResponse.getRequireAuthorizationConsent().equals(false));
        assert (clientRegistrationResponse.getUserUri().contentEquals(SECRET_CLIENT_BASE_URL + "/user"));
        assert (clientRegistrationResponse.getContacts().size() == 2);
        assert (clientRegistrationResponse.getContacts().contains("contact-1"));
        assert (clientRegistrationResponse.getContacts().contains("contact-2"));

        String registrationAccessToken = clientRegistrationResponse.getRegistrationAccessToken();
        String registrationClientUri = clientRegistrationResponse.getRegistrationClientUri();

        ClientRegistrar.ClientRegistrationResponse retrievedClient = registrar.retrieveClient(registrationAccessToken, registrationClientUri);

        assert (retrievedClient.getClientName().contentEquals("secret-client"));
        assert (!Objects.isNull(retrievedClient.getClientId()));
        assert (!Objects.isNull(retrievedClient.getClientSecret()));
        assert (retrievedClient.getScope().contentEquals("read openid write"));
        assert (retrievedClient.getGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue()));
        assert (retrievedClient.getGrantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()));
        assert (retrievedClient.getRedirectUris().contains(SECRET_CLIENT_BASE_URL));
        assert (retrievedClient.getRedirectUris().contains(SECRET_CLIENT_BASE_URL + "/login/oauth2/code/secret-client"));
        assert (retrievedClient.getUserUri().contentEquals(SECRET_CLIENT_BASE_URL + "/user"));
        assert (retrievedClient.getContacts().size() == 2);
        assert (retrievedClient.getContacts().contains("contact-1"));
        assert (retrievedClient.getContacts().contains("contact-2"));
        assert (Objects.isNull(retrievedClient.getRegistrationAccessToken()));
        assert (!retrievedClient.getRegistrationClientUri().isEmpty());

        log.info("Secret Client 注册信息：{}", retrievedClient);
    }
}