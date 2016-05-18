package gmm.service.users;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.User;
import gmm.service.data.DataAccess;

@Service
public class UserService extends UserProvider {

	@Autowired private DataAccess data;
	
	private final SecureRandom random = new SecureRandom();
	
	@Override
	public Collection<User> get() {
		return data.<User>getList(User.class);
	}
	
	public String generatePassword() {
		//toString(32) encodes 5 bits/char, so BigInteger range bits should be a multiple of 5
		return new BigInteger(50, random).toString(32);
	}
	
	public void add(User user) {
		data.add(user);
	}
	
	public void addAll(Collection<User> users) {
		data.addAll(users);
	}
	
	public boolean isUserLoggedIn() {
		return getAuth() != null && getAuth().isAuthenticated() &&
				getAuth().getPrincipal() instanceof org.springframework.security.core.userdetails.User;
	}
	
	public User getLoggedInUser() {
		if (!isUserLoggedIn()) throw new IllegalStateException("User is not logged in!");
		return get(((org.springframework.security.core.userdetails.User)
				getAuth().getPrincipal()).getUsername());
	}
	
	private Authentication getAuth() {
		return SecurityContextHolder.getContext().getAuthentication();
	}
}