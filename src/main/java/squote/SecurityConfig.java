package squote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .inMemoryAuthentication()
                .withUser("thcathy").password("ilovekw").roles("USER");
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	http
    	  	.csrf().disable()
  			.authorizeRequests()                            
  			.antMatchers("/holdingstock/**").hasRole("USER")
  			.anyRequest().permitAll()
  				.and()
  			.formLogin()
  				.and()
  			.httpBasic();
    }
}