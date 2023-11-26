package gmm;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import gmm.service.users.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {
	
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
	
	@Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
        	.csrf(csrf -> {
        		csrf.ignoringRequestMatchers("/plugins/**");
        	})
            .formLogin(formLogin -> {
            	formLogin.loginPage("/login");
            	formLogin.failureHandler(eventAuthenticationFailureHandler());
            })
            .httpBasic(withDefaults());
        
        return http.build();
    }
}
