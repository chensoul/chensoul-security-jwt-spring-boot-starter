package com.chensoul.security.jwt;

import com.chensoul.security.jwt.controller.AuthenticationRestController;
import com.chensoul.security.jwt.filter.TokenAuthenticationFilter;
import com.chensoul.security.jwt.token.TokenHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Slf4j
@Configuration
@ConditionalOnClass(WebSecurityConfigurerAdapter.class)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public TokenHelper tokenHelper(JwtProperties jwtProperties) {
        return new TokenHelper(jwtProperties);
    }

    @Configuration
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
    @ComponentScan(basePackageClasses = AuthenticationRestController.class)
    public static class DefaultAuthApiConfigurer {
        public DefaultAuthApiConfigurer() {
            log.info("Initializing DefaultAuthApiConfigurer");
        }
    }

    @Configuration
    @EnableWebSecurity
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, proxyTargetClass = true)
    @RequiredArgsConstructor
    public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
        private final UserDetailsService userDetailsService;
        private final TokenHelper tokenHelper;
        private final JwtProperties jwtProperties;

        @Bean
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            if (!jwtProperties.isEnabled()) {
                return;
            }

            String[] permitAllPaths = jwtProperties.getPermitAllPaths().toArray(new String[0]);
            http
                    .antMatcher(jwtProperties.getBasePath())
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .authorizeRequests()
                    .antMatchers(permitAllPaths).permitAll()
                    .anyRequest().fullyAuthenticated()
                    .and()
                    .addFilterBefore(tokenAuthenticationFilter(), BasicAuthenticationFilter.class);

            http
                    .csrf().disable();
        }

        private TokenAuthenticationFilter tokenAuthenticationFilter() {
            return new TokenAuthenticationFilter(tokenHelper, userDetailsService);
        }
    }
}
