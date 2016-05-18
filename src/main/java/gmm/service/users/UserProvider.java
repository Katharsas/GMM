package gmm.service.users;

import java.security.Principal;
import java.util.function.Supplier;

import gmm.collections.Collection;
import gmm.domain.UniqueObject;
import gmm.domain.User;

/**
 * Provides methods to access currently loaded users.
 * 
 * @author Jan Mothes
 */
public abstract class UserProvider implements Supplier<Collection<User>> {
	
	/**
	 * @param principal - Represents currently logged in entity (user). Can be retrieved easily,
	 * 		because Spring can inject it into fields or controller methods.
	 * @return The corresponding user object with all user information. Returns Empty user,
	 * 		if argument is null.
	 */
	public User get(Principal principal) {
		return (principal == null) ?
				User.NULL : 
				User.getFromName(get(), principal.getName());
	}
	
	public User get(String name) {
		return User.getFromName(get(), name);
	}
	public User getByIdLink(String idLink) {
		return UniqueObject.getFromIdLink(get(), idLink);
	}
}
