package com.zmyc.common.config;

import com.zmyc.common.filter.LocalCallbackFilter;
import com.zmyc.interceptor.AkSkInterceptor;
import com.zmyc.interceptor.JwtInterceptor;
import com.zmyc.interceptor.RateLimitInterceptor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Web配置
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final AkSkInterceptor akSkInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // 允许所有来源，但仍可使用 credentials
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(akSkInterceptor)
                .addPathPatterns("/**")
                .order(1);

        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**", // 认证相关接口
                        "/api/quartz/**", // 定时任务接口
                        "/swagger-ui/**", // Swagger UI
                        "/v3/api-docs/**", // Swagger API文档
                        "/swagger-ui.html", // Swagger首页
                        "/pay/callback", // 支付回调接口
                        "/error" // 错误页面
                )
                .order(2);
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .order(3);
    }

    @Bean
    public FilterRegistrationBean<ContentCachingFilter> contentCachingFilter() {
        FilterRegistrationBean<ContentCachingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ContentCachingFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<LocalCallbackFilter> localCallbackFilter() {
        FilterRegistrationBean<LocalCallbackFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new LocalCallbackFilter());
        registrationBean.addUrlPatterns("/pay/callback");
        registrationBean.setOrder(2);
        return registrationBean;
    }

    /**
     * 请求体缓存过滤器
     */
    public static class ContentCachingFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                String contentType = httpRequest.getContentType();
                // 跳过 multipart 请求
                if (contentType != null && contentType.startsWith("multipart/")) {
                    chain.doFilter(request, response);
                } else {
                    CachedBodyHttpServletRequest wrapper = new CachedBodyHttpServletRequest(httpRequest);
                    chain.doFilter(wrapper, response);
                }
            } else {
                chain.doFilter(request, response);
            }
        }
    }

    /**
     * 缓存请求体的包装器
     */
    public static class CachedBodyHttpServletRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            // 在构造时就读取并缓存请求体
            java.io.InputStream inputStream = request.getInputStream();
            this.cachedBody = inputStream.readAllBytes();
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            return new CachedBodyServletInputStream(this.cachedBody);
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(
                    new java.io.InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        }

        public byte[] getCachedBody() {
            return cachedBody;
        }
    }

    /**
     * 缓存的输入流
     */
    public static class CachedBodyServletInputStream extends jakarta.servlet.ServletInputStream {
        private final java.io.ByteArrayInputStream cachedBodyInputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.cachedBodyInputStream = new java.io.ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return cachedBodyInputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return cachedBodyInputStream.read();
        }
    }
}
