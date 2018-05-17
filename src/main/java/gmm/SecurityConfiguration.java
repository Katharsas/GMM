package gmm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import gmm.service.users.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
	
	@Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
	
	@Autowired
    public void configureGlobal(
    		AuthenticationManagerBuilder auth,
    		CustomUserDetailsService userDetailsService,
    		PasswordEncoder passwordEncoder) throws Exception  {
        auth
            .userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    }

	@Bean
	AuthenticationFailureHandler eventAuthenticationFailureHandler() {
		return new EventSendingAuthenticationFailureHandler("/login?error");
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
	    http
	    	//.addFilterAfter(new TestDelayFilter(), BasicAuthenticationFilter.class)
	    	.csrf()
	    		.ignoringAntMatchers("/plugins/**")
	    		.and()
	        .formLogin()
	        	.loginPage("/login")
	        	.failureHandler(eventAuthenticationFailureHandler())
	            .and()
	        .httpBasic();
	}
}
