package com.zmyc.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Set;

/**
 * 本地回调过滤器 - 限制支付回调接口只能从本机/内网调用
 */
@Slf4j
public class LocalCallbackFilter implements Filter {

    private static final Set<String> ALLOWED_HOSTS = Set.of("127.0.0.1", "::1");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String remoteAddr = getRealClientIp(httpRequest);

        if (!ALLOWED_HOSTS.contains(remoteAddr)) {
            log.error("回调接口被非法访问拦截: remoteAddr={}, uri={}", remoteAddr, httpRequest.getRequestURI());
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":403,\"message\":\"Forbidden\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getRealClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
