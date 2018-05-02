package squote.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Value(value = "${auth0.cert}")
	private String certificatePath;

	@Override
    protected void configure(HttpSecurity http) throws Exception {
		http
				.cors()
				.and()
				.csrf().disable().authorizeRequests()
				.antMatchers(HttpMethod.GET, "/rest/fund/**").hasRole("admin")
				.antMatchers(HttpMethod.GET, "/rest/createholding/**").hasRole("admin")
				.antMatchers(HttpMethod.GET, "/rest/stock/holding/**").hasRole("admin")
				.anyRequest().permitAll()
				.and()
				.addFilter(new JWTAuthorizationFilter(authenticationManager(), certificatePath))
				// this disables session creation on Spring Security
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.and()
				.exceptionHandling()
				.authenticationEntryPoint(new org.springframework.boot.autoconfigure.security.Http401AuthenticationEntryPoint("headerValue"));;
    }

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedHeaders(Arrays.asList("*"));
		configuration.setAllowedOrigins(Arrays.asList("*"));
		configuration.setAllowedMethods(Arrays.asList("GET","POST"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
