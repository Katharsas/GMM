package gmm.service;

import gmm.domain.NamedObject;
import gmm.util.HashSet;
import gmm.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService{

	@Autowired
	UserService users;
	
	@Override
	public UserDetails loadUserByUsername(String name)
			throws UsernameNotFoundException {
		
		gmm.domain.User user = NamedObject.getFromName(users.get(), name);
		if(user == null || user.getPasswordHash() == null) {
			throw new UsernameNotFoundException("Could not find User with name "+name);
		}
		
		User wrapper = new User(user.getName(), user.getPasswordHash(), getAuthorities(user));
		return wrapper;
	}
	
	private Set<GrantedAuthority> getAuthorities(gmm.domain.User user) {
		Set<GrantedAuthority> auths = new HashSet<>();
		switch (user.getRole()) {
		case "ROLE_ADMIN":
			auths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		case "ROLE_USER":
			auths.add(new SimpleGrantedAuthority("ROLE_USER"));
		case "ROLE_GUEST":
			auths.add(new SimpleGrantedAuthority("ROLE_GUEST"));
		default:
			break;
		}
		return auths;
	}

}
