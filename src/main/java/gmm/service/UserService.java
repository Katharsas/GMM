package gmm.service;

import gmm.domain.User;
import gmm.service.data.DataAccess;
import gmm.util.Collection;

import java.security.Principal;

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
		return User.getFromName(data.<User>getList(User.class), name);
	}
}
