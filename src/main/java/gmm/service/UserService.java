package gmm.service;

import gmm.domain.NamedObject;
import gmm.domain.UniqueObject;
import gmm.domain.User;
import gmm.service.data.DataAccess;
import gmm.util.Collection;

import java.math.BigInteger;
import java.security.Principal;
import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	@Autowired
	DataAccess data;

	public User get(Principal principal) {
		return (principal == null) ?
				User.NULL : 
				User.getFromName(data.<User>getList(User.class), principal.getName());
	}
	
	public Collection<User> get() {
		return data.<User>getList(User.class);
	}
	
	public User get(String name) {
		return NamedObject.getFromName(data.<User>getList(User.class), name);
	}
	
	public User getByIdLink(String idLink) {
		return UniqueObject.getFromId(data.<User>getList(User.class), idLink);
	}
	
	public String generatePassword() {
		SecureRandom random = new SecureRandom();
		return new BigInteger(50, random).toString(32);
	}
}
