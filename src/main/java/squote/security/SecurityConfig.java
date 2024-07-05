package squote.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Value(value = "${auth0.cert}")
	private String certificatePath;

	@Value(value = "${auth0.audience}")
	private String jwtAudience;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(CsrfConfigurer::disable)
			.authorizeHttpRequests(auth ->
					auth
							.requestMatchers(HttpMethod.GET, "/rest/fund/**").authenticated()
							.requestMatchers(HttpMethod.GET, "/rest/createholding/**").authenticated()
							.requestMatchers(HttpMethod.GET, "/rest/stock/holding/**").authenticated()
							.anyRequest().permitAll()
			)
//			.addFilter(new JWTAuthorizationFilter(authenticationManager, certificatePath, jwtAudience))
			.sessionManagement(sessionManagement ->
					sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.exceptionHandling(exceptionHandling ->
					exceptionHandling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
			);
		
		http.apply(new JWTFilterConfigurer(certificatePath, jwtAudience));
		return http.build();
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
					.allowedOrigins("*")
					.allowedMethods("*")
					.allowedHeaders("*");
			}
		};
	}

	@Bean
	public InMemoryUserDetailsManager userDetailsManager() {
		return new InMemoryUserDetailsManager();
	}
	
}
