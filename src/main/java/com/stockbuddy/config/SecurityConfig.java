//src/main/java/com/stockbuddy/config/SecurityConfig.java
package com.stockbuddy.config;

import com.stockbuddy.repository.SessionRepository;
import com.stockbuddy.security.JwtAuthFilter;
import com.stockbuddy.security.JwtUtil;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public JwtAuthFilter jwtAuthFilter(JwtUtil jwtUtil, SessionRepository sessionRepository) {
		return new JwtAuthFilter(jwtUtil, sessionRepository);
	}

	/**
	 * {@link JwtAuthFilter} is a {@code Filter}; as a plain {@code @Bean} Spring Boot
	 * would register it on the servlet container as well as in Security. Disable servlet
	 * registration so only the security filter chain runs it (avoids odd ordering with
	 * public endpoints).
	 */
	@Bean
	public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterServletRegistration(JwtAuthFilter jwtAuthFilter) {
		FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(jwtAuthFilter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
		http
				// Disable CSRF (stateless REST API)
				.csrf(AbstractHttpConfigurer::disable)

				// CORS integrated with security chain (OPTIONS preflight has no JWT)
				.cors(Customizer.withDefaults())

				// Session management — stateless (JWT)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// Authorization rules
				.authorizeHttpRequests(auth -> auth
						// Public endpoints

						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

						.requestMatchers(HttpMethod.POST, "/api/register").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/login").permitAll()
						.requestMatchers("/api/forgot-password/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/captcha").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/verify-captcha").permitAll()
						.requestMatchers(HttpMethod.GET, "/").permitAll()
						// Everything else requires authentication
						
						 // ── Authenticated: profile, sessions, preferences ───
		                .requestMatchers("/api/profile", "/api/profile/**").authenticated()
		                .requestMatchers("/api/sessions", "/api/sessions/**").authenticated()
		                .requestMatchers("/api/preferences", "/api/preferences/**").authenticated()
		                .requestMatchers("/api/2fa", "/api/2fa/**").authenticated()
		                .requestMatchers("/api/watchlist", "/api/watchlist/**").authenticated()
		                .requestMatchers("/api/predictions/**").authenticated()
		                .requestMatchers("/api/demotrading/**").authenticated()
		                .requestMatchers("/api/change-password").authenticated()
		                .requestMatchers("/api/admin/**").authenticated()
						.anyRequest().authenticated())

				// Add JWT filter before Spring's username/password filter
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * BCrypt password encoder — mirrors bcryptjs.genSalt(10) / bcrypt.hash()
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(10);
	}
}
