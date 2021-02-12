package squote.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuthenticationService {

    public Optional<String> getUserId() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                    .flatMap(authentication -> Optional.ofNullable(authentication.getName()));
    }
}
