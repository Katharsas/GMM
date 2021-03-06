package gmm.service.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import gmm.collections.HashSet;
import gmm.collections.Set;
import gmm.service.data.DataAccess;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
	DataAccess data;
	
	@Override
	public UserDetails loadUserByUsername(String name)
			throws UsernameNotFoundException {
		
		
		final gmm.domain.User user = gmm.domain.User.getFromName(data.getList(gmm.domain.User.class), name);
		if(user == null || user.getPasswordHash() == null || !user.isEnabled()) {
			throw new UsernameNotFoundException("Could not find User with name "+name);
		}
		
		final User wrapper = new User(user.getName(), user.getPasswordHash(), getAuthorities(user));
		return wrapper;
	}
	
	private Set<GrantedAuthority> getAuthorities(gmm.domain.User user) {
		final Set<GrantedAuthority> auths = new HashSet<>(GrantedAuthority.class);
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
