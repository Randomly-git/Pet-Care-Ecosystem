package com.petcare.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：CORS 跨域 + JWT 认证拦截
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    // ======== CORS 配置 ========
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("✅ CORS配置已加载 - 允许特定来源跨域访问");
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    // ======== JWT 拦截器注册 ========
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")              // 拦截所有 API
                .excludePathPatterns(
                        "/api/auth/**",                   // 放行登录/注册
                        "/api/media/**",                  // 媒体文件由 media-backend 处理
                        "/api/stats/**",                  // 统计接口（可选保护）
                        "/api/llm/**"                     // LLM 接口（可选保护）
                )
                .order(1);

        log.info("✅ JWT认证拦截器已注册 - 拦截 /api/**（排除 /api/auth/**）");
    }
}
