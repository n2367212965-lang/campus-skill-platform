package com.example.campusskillplatform.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * 全局跨域资源配置
 * 解决前后端分离架构下的跨域问题
 * 优先级设置为最高，确保在所有拦截器之前执行
 */
@Configuration
public class CorsFilterConfig {

    /**
     * 配置CORS过滤器
     * 允许前端Vue应用（localhost:5173）访问后端API（localhost:8080）
     * @return 注册好的CORS过滤器
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许的源地址（开发环境支持所有localhost端口）
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",    // 允许所有localhost端口（开发环境灵活切换）
                "http://127.0.0.1:*"    // 兼容IP访问
        ));

        // 允许所有请求头（包括Authorization、Content-Type等）
        config.addAllowedHeader("*");

        // 允许所有HTTP方法（GET、POST、PUT、DELETE等）
        config.addAllowedMethod("*");

        // 允许携带凭证（Token、Cookie等）
        config.setAllowCredentials(true);

        // 预检请求（OPTIONS）缓存时间：1小时，减少重复预检
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径生效
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        // 设置最高优先级，确保在Spring Security和自定义拦截器之前执行
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return bean;
    }
}
