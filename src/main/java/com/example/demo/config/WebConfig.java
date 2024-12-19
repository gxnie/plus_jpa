package com.example.demo.config;

import com.example.demo.entity.Role;
import com.example.demo.filter.AuthFilter;
import com.example.demo.filter.RoleFilter;
import com.example.demo.interceptor.AdminRoleInterceptor;
import com.example.demo.interceptor.AuthInterceptor;
import com.example.demo.interceptor.UserRoleInterceptor;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    // TODO: 2. 인가에 대한 이해
    private static final String[] AUTH_REQUIRED_PATH_PATTERNS = {"/users/logout", "/admins/*", "/items/*"};
    private static final String[] USER_ROLE_REQUIRED_PATH_PATTERNS = {"/reservations/*"};

    // ADMIN 권한을 만들어, 해당 권한 아니면 요청할 수 없게함
    private static final String[] ADMIN_ROLE_REQUIRED_PATH_PATTERNS = {"/admins/*"};

    private final AuthInterceptor authInterceptor;
    private final UserRoleInterceptor userRoleInterceptor;
    private final AdminRoleInterceptor adminRoleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(AUTH_REQUIRED_PATH_PATTERNS)
                .order(Ordered.HIGHEST_PRECEDENCE);

        registry.addInterceptor(userRoleInterceptor)
                .addPathPatterns(USER_ROLE_REQUIRED_PATH_PATTERNS)
                .order(Ordered.HIGHEST_PRECEDENCE + 1);

        registry.addInterceptor(adminRoleInterceptor)
                .addPathPatterns(ADMIN_ROLE_REQUIRED_PATH_PATTERNS)
                .order(Ordered.HIGHEST_PRECEDENCE + 2);
    }

/**
 * Filter와 Interceptor가 같은 경로에 대해 동일한 인증 및 권한 검사를해서 주석처리
 * */

//    @Bean
//    public FilterRegistrationBean authFilter() {
//        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
//        filterRegistrationBean.setFilter(new AuthFilter());
//        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
//        filterRegistrationBean.addUrlPatterns(AUTH_REQUIRED_PATH_PATTERNS);
//        return filterRegistrationBean;
//    }
//
//    @Bean
//    public FilterRegistrationBean userRoleFilter() {
//        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
//        filterRegistrationBean.setFilter(new RoleFilter(Role.USER));
//        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
//        filterRegistrationBean.addUrlPatterns(USER_ROLE_REQUIRED_PATH_PATTERNS);
//        return filterRegistrationBean;
//    }

}