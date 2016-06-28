package gmm.service.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.domain.User;

@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class CurrentUser {

	private final User loggedInUser;
	@Autowired
	public CurrentUser(UserService users) {
		this.loggedInUser = users.getLoggedInUser();
	}
	public User get() {
		return loggedInUser;
	}
}
