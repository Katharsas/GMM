package gmm.service.users;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.User;
import gmm.service.data.DataAccess;

@Service
public class UserService extends UserProvider {

	private final DataAccess data;
	private final SecureRandom random;
	
	@Autowired
	public UserService(DataAccess data) {
		super(() -> data.<User>getList(User.class));
		this.data = data;
		random = new SecureRandom();
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
}
