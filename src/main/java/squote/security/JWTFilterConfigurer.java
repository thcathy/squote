package squote.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

public class JWTFilterConfigurer extends AbstractHttpConfigurer<JWTFilterConfigurer, HttpSecurity> {
    private String certificatePath;
    private String jwtAudience;

    public JWTFilterConfigurer(String certificatePath, String jwtAudience) {

        this.certificatePath = certificatePath;
        this.jwtAudience = jwtAudience;
    }

    @Override
    public void configure(HttpSecurity http) {
        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);
        http.addFilter(new JWTAuthorizationFilter(authenticationManager, certificatePath, jwtAudience));
    }
}
