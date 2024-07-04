package squote.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.lang.Collections;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class JWTAuthorizationFilter extends BasicAuthenticationFilter {
    public static final String CLAIM_ROLE_KEY = "https://squote.funfunspell.com/roles";

    private static Logger log = LoggerFactory.getLogger(JWTAuthorizationFilter.class);

    private PublicKey key;
    private String expectedAudiance;

    public JWTAuthorizationFilter(AuthenticationManager authManager, String certificatePath, String expectedAudiance) {
        super(authManager);

        try {
            CertificateFactory fact = null;
            fact = CertificateFactory.getInstance("X.509");
            InputStream is =  this.getClass().getResourceAsStream(certificatePath);
            X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
            this.key = cer.getPublicKey();
            this.expectedAudiance = expectedAudiance;
        } catch (Exception e) {
            log.error("error in reading certificate: ", e);
        }
    }


    @Override
    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res,
                                    jakarta.servlet.FilterChain chain) throws IOException, ServletException {
        String header = req.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer")) {
            chain.doFilter(req, res);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = getAuthentication(req);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(req, res);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(jakarta.servlet.http.HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null) {
                // parse the token.
                Claims claims = Jwts.parser().setSigningKey(key)
                        .parseClaimsJws(token.replace("Bearer", "").replaceAll("\"",""))
                        .getBody();

                if (isNotValidAudience(claims.getAudience())) return null;
                if (claims.getExpiration().getTime() < new Date().getTime()) return null;

                String user = claims.getSubject();
                List<GrantedAuthority> roles = extractRoles((List<String>) claims.get(CLAIM_ROLE_KEY));
                if (user != null) {
                    return new UsernamePasswordAuthenticationToken(user, null, roles);
                }

                return null;
            }
        } catch (ExpiredJwtException e) {
            log.info(e.toString());
        } catch (Exception e) {
            log.error("Error in processing JWT:", e);
        }
        return null;
    }

    private boolean isNotValidAudience(String audience) {
        return !expectedAudiance.equals(audience);
    }

    private List<GrantedAuthority> extractRoles(List<String> claims) {
        if (Collections.isEmpty(claims)) return new ArrayList<>();

        return claims.stream().map(s -> new SimpleGrantedAuthority("ROLE_" + s))
                .collect(Collectors.toList());
    }
}
