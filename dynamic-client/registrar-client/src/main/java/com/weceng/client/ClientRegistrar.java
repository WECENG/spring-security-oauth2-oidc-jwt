package com.weceng.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.oauth2.sdk.client.ClientRegistrationRequest;
import com.nimbusds.oauth2.sdk.client.ClientRegistrationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * <p>
 * 客户端注册器
 * </p>
 *
 * @author chenwc@tsintergy.com
 * @since 2024/10/28 09:39
 */
public class ClientRegistrar {

    private final WebClient webClient;

    public ClientRegistrar(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * 属性参考
     * {@link org.springframework.security.oauth2.server.authorization.oidc.OidcClientMetadataClaimNames}
     * {@link org.springframework.security.oauth2.server.authorization.settings.ClientSettings}
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClientRegistrationRequest {

        @JsonProperty("client_name")
        private String clientName;

        /**
         * 注册时不指定会默认生成
         * @see org.springframework.security.oauth2.server.authorization.oidc.converter.OidcClientRegistrationRegisteredClientConverter#convert(org.springframework.security.oauth2.server.authorization.oidc.OidcClientRegistration)
         */
        @JsonProperty("client_id")
        private String clientId;

        /**
         * 注册时不指定会默认生成
         * @see org.springframework.security.oauth2.server.authorization.oidc.converter.OidcClientRegistrationRegisteredClientConverter#convert(org.springframework.security.oauth2.server.authorization.oidc.OidcClientRegistration)
         */
        @JsonProperty("client_secret")
        private String clientSecret;

        @JsonProperty("grant_types")
        private List<String> grantTypes;

        @JsonProperty("redirect_uris")
        private List<String> redirectUris;

        @JsonProperty("token_endpoint_auth_method")
        private List<String> clientAuthenticationMethods;

        /**
         * 是否开启PKCE
         */
        @JsonProperty("settings.client.require-proof-key")
        private Boolean requireProofKey;

        /**
         * 是否开启客户端授权确认
         */
        @JsonProperty("settings.client.require-authorization-consent")
        private Boolean requireAuthorizationConsent;

        @JsonProperty("user_uri")
        private String userUri;

        private List<String> contacts;

        private String scope;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClientRegistrationResponse {

        @JsonProperty("registration_access_token")
        private String registrationAccessToken;

        @JsonProperty("registration_client_uri")
        private String registrationClientUri;

        @JsonProperty("client_name")
        private String clientName;

        /**
         * auth server的转换器中是自动生成的
         */
        @JsonProperty("client_id")
        private String clientId;

        /**
         * auth server的转换器中是自动生成的
         * 如果有加密算法，注册成功后，输出的是原文；通过client_id查看客户端输出的是密文
         */
        @JsonProperty("client_secret")
        private String clientSecret;

        @JsonProperty("grant_types")
        private List<String> grantTypes;

        @JsonProperty("redirect_uris")
        private List<String> redirectUris;

        @JsonProperty("client_authentication_methods")
        private List<String> clientAuthenticationMethods;

        /**
         * 是否开启PKCE
         */
        @JsonProperty("settings.client.require-proof-key")
        private Boolean requireProofKey;

        /**
         * 是否开启客户端授权确认
         */
        @JsonProperty("settings.client.require-authorization-consent")
        private Boolean requireAuthorizationConsent;

        @JsonProperty("user_uri")
        private String userUri;

        private List<String> contacts;

        private String scope;
    }

    /**
     * 注册客户端
     *
     * @param initialAccessToken access token
     * @param request         请求对象
     * @return 注册结果
     */
    public ClientRegistrationResponse registerClient(String initialAccessToken, ClientRegistrationRequest request) {
        return this.webClient
                .post()
                .uri("http://127.0.0.1:9000/connect/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(initialAccessToken))
                .body(Mono.just(request), ClientRegistrationRequest.class)
                .retrieve()
                .bodyToMono(ClientRegistrationResponse.class)
                .block();
    }

    /**
     * 获取客户端注册信息
     * @param registrationAccessToken 注册 access token
     * @param registrationClientUri 注册客户端uri
     * @return 注册客户端信息
     */
    public ClientRegistrationResponse retrieveClient(String registrationAccessToken, String registrationClientUri) {
        return this.webClient
                .get()
                .uri(registrationClientUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(registrationAccessToken))
                .retrieve()
                .bodyToMono(ClientRegistrationResponse.class)
                .block();
    }

}
