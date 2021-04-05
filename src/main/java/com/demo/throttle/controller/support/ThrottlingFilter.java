package com.demo.throttle.controller.support;

import com.demo.throttle.service.ThrottlingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
public class ThrottlingFilter implements Filter {
    public static final String USER_TOKEN = "User-Token";
    private final ThrottlingService throttlingService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        Optional<String> token = Optional.ofNullable(request.getHeader(USER_TOKEN));
        if (throttlingService.tryAcquireRequestPermit(token)) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Your throttling limit is exceeded");
        }
    }
}
