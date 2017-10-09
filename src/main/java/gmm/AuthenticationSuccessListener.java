package gmm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import gmm.domain.User;
import gmm.service.data.DataAccess;
import gmm.service.users.UserProvider;

@Component
public class AuthenticationSuccessListener implements
		ApplicationListener<InteractiveAuthenticationSuccessEvent> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final UserProvider users;
	
	@Autowired
	public AuthenticationSuccessListener(DataAccess data) {
		users = new UserProvider(() -> data.getList(User.class));
	}
	
    @Override
    public void onApplicationEvent(InteractiveAuthenticationSuccessEvent event) {
    	
    	final User user = users.get(event.getAuthentication().getName());
    	final boolean isAdmin = user.getRole().equals(User.ROLE_ADMIN);
    	logger.info((isAdmin ? "Admin" : "User") + " with id " + user.getIdLink()
    			+ " has successfully logged in!");
    }
}