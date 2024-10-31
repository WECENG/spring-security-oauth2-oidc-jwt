package com.weceng.client.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * <p>
 * 配置类
 * </p>
 *
 * @author chenwc@tsintergy.com
 * @since 2024/10/28 10:56
 */
@Configuration
public class RegistrarClientConfig {

    @Bean
    public WebClient webClient(){
        return WebClient.builder().build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth ->
                        auth.requestMatchers("/").permitAll()
                                .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .oauth2Client(Customizer.withDefaults());
        return http.build();
    }

}
