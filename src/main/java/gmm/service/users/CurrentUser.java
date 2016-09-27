package gmm.service.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.domain.User;



@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class CurrentUser {

	private boolean isUserLoggedIn = false;
	private User loggedInUser;
	
	private final UserService users;
	
	@Autowired
	public CurrentUser(UserService users) {
		this.users = users;
		update();
	}
	
	private void update() {
		final boolean before = isUserLoggedIn;
		isUserLoggedIn = UserProvider.isUserLoggedIn();
		if (isUserLoggedIn != before) {
			loggedInUser = isUserLoggedIn ? users.getLoggedInUser() : null;
		}
	}
	
	public User get() {
		if (isUserLoggedIn) {
			return loggedInUser;
		} else {
			throw new IllegalStateException("User is not logged in!");
		}
	}
	
	public boolean isLoggedIn() {
		update();
		return isUserLoggedIn;
	}
}
