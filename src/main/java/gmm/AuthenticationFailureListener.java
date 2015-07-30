package gmm;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFailureListener
		implements ApplicationListener<AuthenticationFailureBadCredentialsEvent>{

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
		String name = event.getAuthentication().getName();
		logger.warn("Login attempt failed due to wrong username or password!"
				+ " Name used: " + name);
	}
}
