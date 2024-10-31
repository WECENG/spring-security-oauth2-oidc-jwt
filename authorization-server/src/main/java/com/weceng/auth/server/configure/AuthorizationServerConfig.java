package com.weceng.auth.server.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;

import static com.weceng.auth.server.configure.DynamicClientMetadataConfig.dynamicClientMetadataProviders;

/**
 * <p>
 * 认证授权服务配置类
 * </p>
 *
 * @author WECENG
 * @since 2024/10/17 17:16
 */
@Configuration
public class AuthorizationServerConfig {

    /**
     * 客户端注册
     *
     * @return 客户端注册信息
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient registeredLoginClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("login_client")
                .clientSecret("{noop}client_secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:8001/login/oauth2/code/login-client")
                .redirectUri("http://127.0.0.1:8001/authorized")
                .scope("read")
                .scope("write")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(3))
                        .build())
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .build();

        RegisteredClient registeredClientA = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("web-client-a")
                .clientSecret("{noop}web-client-a-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://oauth2clienta.com:8002/login/oauth2/code/web-client-a")
                .redirectUri("http://oauth2clienta.com:8002/authorized")
                .scope("read")
                .scope("write")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(3))
                        .build())
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();

        RegisteredClient registeredClientB = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("web-client-b")
                .clientSecret("{noop}web-client-b-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://oauth2clientb.com:8003/login/oauth2/code/web-client-b")
                .redirectUri("http://oauth2clientb.com:8003/authorized")
                .scope("read")
                .scope("write")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(3))
                        .build())
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();

        RegisteredClient registrarClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("registrar-client")
                .clientSecret("{noop}registrar-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://oauth2registrar.com:8004/login/oauth2/code/registrar-client")
                .redirectUri("http://oauth2registrar.com:8004/authorized")
                .scope("read")
                .scope("write")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                //必须包含该scope才可用该注册器注册其他客户端
                .scope("client.create")
                .scope("client.read")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(3))
                        .build())
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();

        return new InMemoryRegisteredClientRepository(registeredLoginClient, registeredClientA, registeredClientB, registrarClient);
    }

    /**
     * 用户信息（不加密）
     *
     * @return 用户信息
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails userDetails = User.builder()
                .username("admin")
                .password("{noop}admin")
                .roles("ADMIN")
                .passwordEncoder(Function.identity())
                .build();
        return new InMemoryUserDetailsManager(userDetails);
    }

    /**
     * 授权服务配置
     *
     * @return 配置
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://oauth2server.com:9000")
                .build();
    }

    /**
     * authorization server web安全配置
     *
     * @param http http
     * @return web安全配置链
     * @throws Exception 异常
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(oidc -> oidc.clientRegistrationEndpoint(clientRegistrationEndpoint ->
                        clientRegistrationEndpoint.authenticationProviders(dynamicClientMetadataProviders())));
        http.exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                )
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * 标准 web安全配置
     *
     * @param http http
     * @return web安全链
     * @throws Exception 异常
     */
    @Bean
    @Order(2)
    public SecurityFilterChain standardSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults());

        return http.build();
    }


}
