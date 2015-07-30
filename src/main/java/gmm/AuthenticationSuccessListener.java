package gmm;

import gmm.domain.User;
import gmm.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessListener implements
		ApplicationListener<InteractiveAuthenticationSuccessEvent> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired private UserService users;
	
    @Override
    public void onApplicationEvent(InteractiveAuthenticationSuccessEvent event) {
    	User user = users.get(event.getAuthentication().getName());
    	boolean isAdmin = user.getRole().equals(User.ROLE_ADMIN);
    	logger.info((isAdmin ? "Admin" : "User") + " with id " + user.getIdLink()
    			+ " has successfully logged in!");
    }
}