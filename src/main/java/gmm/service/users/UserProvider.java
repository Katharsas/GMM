package gmm.service.users;

import java.util.function.Supplier;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import gmm.collections.Collection;
import gmm.domain.UniqueObject;
import gmm.domain.User;
import gmm.service.data.DataAccess;

/**
 * Provides methods to access currently loaded users.
 * 
 * @author Jan Mothes
 */
public class UserProvider implements Supplier<Collection<User>> {
	
	Supplier<Collection<User>> getUsers;
	
	public UserProvider(Supplier<Collection<User>> getUsers) {
		this.getUsers = getUsers;
	}
	
	@Override
	public Collection<User> get() {
		return getUsers.get();
	}
	
	public User get(String name) {
		return User.getFromName(get(), name);
	}
	
	public static User get(String name, DataAccess data) {
		return User.getFromName(data.getList(User.class), name);
	}
	
	public User getByIdLink(String idLink) {
		return UniqueObject.getFromIdLink(get(), idLink);
	}
	
	public static boolean isUserLoggedIn() {
		return isAuthenticated(getAuth());
	}
	
	public User getLoggedInUser() {
		return getLoggedInUser(get());
	}
	
	private static User getLoggedInUser(Collection<User> users) {
		final Authentication auth = getAuth();
		if (!isUserLoggedIn(auth)) throw new IllegalStateException("User is not logged in!");
		return User.getFromName(users, auth.getName());
	}
	
	/**
	 * Finds a user that can be linked to the current thread execution. This is the logged in user
	 * if called from a request session thread or "SYSTEM" if called from a non-session thread.
	 * 
	 * @return Always a user object, never null.
	 */
	public static User getExecutingUser(Collection<User> users) {
		final Authentication auth = getAuth();
		if (auth == null) {
			return User.SYSTEM;
		} else {
			if (isUserLoggedIn(auth)) {
				final User user = getLoggedInUser(users);
				return user == null ? User.NULL : user;
			} else {
				return User.UNKNOWN;
			}
		}
	}
	
	private static boolean isUserLoggedIn(Authentication auth) {
		return auth != null && isAuthenticated(auth);
	}
	
	private static boolean isAuthenticated(Authentication auth) {
		return auth.isAuthenticated() &&
				auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User;
	}
	
	private static Authentication getAuth() {
		return SecurityContextHolder.getContext().getAuthentication();
	}
}
