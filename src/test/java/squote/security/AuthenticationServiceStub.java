package squote.security;

import java.util.Optional;
import java.util.UUID;

public class AuthenticationServiceStub extends AuthenticationService {
    final public String TESTER_USERID = UUID.randomUUID().toString();

    public String userId = TESTER_USERID;

    @Override
    public Optional<String> getUserId() {
        return Optional.of(userId);
    }
}
