package com.weceng.auth.server.configure;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.oidc.OidcClientRegistration;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcClientConfigurationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcClientRegistrationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.oidc.converter.OidcClientRegistrationRegisteredClientConverter;
import org.springframework.security.oauth2.server.authorization.oidc.converter.RegisteredClientOidcClientRegistrationConverter;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 动态客户端元信息配置类
 * </p>
 *
 * @author chenwc@tsintergy.com
 * @since 2024/10/25 14:44
 */
public class DynamicClientMetadataConfig {

    /**
     * 客户端元数据
     * 希望替换原有属性参考 {@link org.springframework.security.oauth2.server.authorization.oidc.OidcClientMetadataClaimNames}
     *
     * @return provider
     */
    public static Consumer<List<AuthenticationProvider>> dynamicClientMetadataProviders() {
        List<String> customClientMetadata = List.of("settings.client.require-authorization-consent", "settings.client.require-proof-key", "user_uri", "contacts");
        return authenticationProviders -> {
            DynamicRegisteredClientConvertor registeredClientConvertor = new DynamicRegisteredClientConvertor(customClientMetadata, new OidcClientRegistrationRegisteredClientConverter());
            DynamicClientRegistrationConverter clientRegistrationConverter = new DynamicClientRegistrationConverter(customClientMetadata, new RegisteredClientOidcClientRegistrationConverter());
            authenticationProviders.forEach(authenticationProvider -> {
                if (authenticationProvider instanceof OidcClientRegistrationAuthenticationProvider provider) {
                    provider.setClientRegistrationConverter(clientRegistrationConverter);
                    provider.setRegisteredClientConverter(registeredClientConvertor);
                }
                if (authenticationProvider instanceof OidcClientConfigurationAuthenticationProvider provider) {
                    provider.setClientRegistrationConverter(clientRegistrationConverter);
                }
            });
        };
    }

    /**
     * 客户端转换器
     */
    private static class DynamicRegisteredClientConvertor implements Converter<OidcClientRegistration, RegisteredClient> {

        /**
         * 客户端自定义信息
         */
        private final List<String> customClientMetadata;

        /**
         * 该转换器默认会启动PKCE和CONSENT
         * {@link OidcClientRegistrationRegisteredClientConverter#convert(OidcClientRegistration)}方法（具体实现见第106行）
         */
        private final OidcClientRegistrationRegisteredClientConverter delegate;

        public DynamicRegisteredClientConvertor(List<String> customClientMetadata,
                                                OidcClientRegistrationRegisteredClientConverter delegate) {
            this.customClientMetadata = customClientMetadata;
            this.delegate = delegate;
        }

        @Override
        public RegisteredClient convert(@NonNull OidcClientRegistration clientRegistration) {
            RegisteredClient registeredClient = this.delegate.convert(clientRegistration);
            ClientSettings.Builder clientSettingsBuilder = ClientSettings.withSettings(registeredClient.getClientSettings().getSettings());
            if (!CollectionUtils.isEmpty(customClientMetadata)) {
                clientRegistration.getClaims().forEach((claim, value) -> {
                    if (this.customClientMetadata.contains(claim)) {
                        clientSettingsBuilder.setting(claim, value);
                    }
                });
            }
            //公共应用public client,启用PKCE，需要设置None。但是已不支持动态注册public client.
            // issue: https://github.com/spring-projects/spring-authorization-server/issues/625
            // 手动设置，不建议
            return RegisteredClient.from(registeredClient)
                    .clientAuthenticationMethods(authenticationMethods -> {
                        if (registeredClient.getClientSettings().isRequireProofKey()) {
                            authenticationMethods.add(ClientAuthenticationMethod.NONE);
                        }
                    })
                    //如果存在自定义则使用
                    .clientId(Optional.of(clientRegistration.getClientId()).orElse(registeredClient.getClientId()))
                    .clientSecret(Optional.of(clientRegistration.getClientSecret()).orElse(registeredClient.getClientSecret()))
                    .clientSettings(clientSettingsBuilder.build())
                    .build();
        }
    }

    /**
     * 客户端仓库转换器
     */
    private static class DynamicClientRegistrationConverter implements Converter<RegisteredClient, OidcClientRegistration> {
        /**
         * 客户端自定义信息
         */
        private final List<String> customClientMetadata;

        /**
         * 该转换器默认会启动PKCE和CONSENT
         * {@link OidcClientRegistrationRegisteredClientConverter#convert(OidcClientRegistration)}方法（具体实现见第106行）
         */
        private final RegisteredClientOidcClientRegistrationConverter delegate;

        public DynamicClientRegistrationConverter(List<String> customClientMetadata,
                                                  RegisteredClientOidcClientRegistrationConverter delegate) {
            this.customClientMetadata = customClientMetadata;
            this.delegate = delegate;
        }

        @Override
        public OidcClientRegistration convert(@NonNull RegisteredClient registeredClient) {
            OidcClientRegistration clientRegistration = this.delegate.convert(registeredClient);
            HashMap<String, Object> claims = new HashMap<>(clientRegistration.getClaims());
            if (!CollectionUtils.isEmpty(customClientMetadata)) {
                ClientSettings clientSettings = registeredClient.getClientSettings();
                Map<String, Object> customClientMetadataMap = customClientMetadata.stream()
                        .filter(metadata -> clientSettings.getSetting(metadata) != null)
                        .collect(Collectors.toMap(Function.identity(), clientSettings::getSetting));
                claims.putAll(customClientMetadataMap);
            }
            return OidcClientRegistration.withClaims(claims).build();
        }
    }

}
