package com.alansf.apexzombiekiller.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ensures X-Frame-Options header allows iframe embedding for Salesforce LWC.
 * By default, Spring Boot may set X-Frame-Options: DENY which blocks iframe embedding.
 */
@Configuration
public class FrameOptionsConfig {
	@Bean
	public Filter frameOptionsFilter() {
		return new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
					throws ServletException, IOException {
				// Remove any existing X-Frame-Options header
				response.setHeader("X-Frame-Options", "SAMEORIGIN");
				// Also set Content-Security-Policy to allow framing from Salesforce
				response.setHeader("Content-Security-Policy", "frame-ancestors 'self' https://*.salesforce.com https://*.force.com;");
				filterChain.doFilter(request, response);
			}
		};
	}
}

