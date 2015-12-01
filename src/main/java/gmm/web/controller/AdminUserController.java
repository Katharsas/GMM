package gmm.web.controller;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.collections.Collection;
import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.data.XMLService;
import gmm.web.sessions.TaskSession;

@Controller
@RequestMapping("admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")

public class AdminUserController {

	@Autowired TaskSession session;
	
	@Autowired DataConfigService config;
	@Autowired DataAccess data;
	@Autowired FileService fileService;
	@Autowired XMLService xmlService;
	@Autowired UserService users;
	@Autowired PasswordEncoder encoder;
	
	/**
	 * Edit User
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = {"/users/edit/{idLink}"}, method = RequestMethod.POST)
	public @ResponseBody void editUser(
			@PathVariable("idLink") String idLink,
			@RequestParam(value="name", required=false) String name,
			@RequestParam(value="role", required=false) String role) {
		
		final User exists = User.getFromName(users.get(), name);
		if(idLink.equals("new")) {
			final User newUser = new User(name);
			if (exists != null) {
				throw new IllegalArgumentException(
						"Can't add user. A user with this name already exists!");
			}
			data.add(newUser);
		}
		else {
			final User user = users.getByIdLink(idLink);
			if(name != null) {
				if (exists != null && user != exists) {
					throw new IllegalArgumentException(
							"Can't change name. Another user with this name already exists!");
				}
				user.setName(name);
			}
			if(role != null) user.setRole(role);
		}
	}
	
	/**
	 * Enable/Disable User Admin Role
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = {"/users/admin/{idLink}"}, method = RequestMethod.POST)
	public @ResponseBody void switchAdmin(@PathVariable("idLink") String idLink) {
		
		final User user = users.getByIdLink(idLink);
		user.setRole(user.getRole().equals(User.ROLE_ADMIN) ? 
				User.ROLE_USER : User.ROLE_ADMIN);
	}
	
	/**
	 * Generate User Password
	 * -----------------------------------------------------------------
	 * This resets the users password to a new randomly generated password.
	 */
	@RequestMapping(value = "/users/reset/{idLink}")
	public @ResponseBody String[] resetPassword(@PathVariable("idLink") String idLink) {
		
		final User user = users.getByIdLink(idLink);
		final String password = users.generatePassword();
		user.setPasswordHash(encoder.encode(password));
		return new String[] {password};
	}
	
	/**
	 * Save Users
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = "/users/save", method = RequestMethod.POST)
	public @ResponseBody void saveUsers() throws IOException {
		
		final Path path = config.USERS.resolve("users.xml");
		fileService.prepareFileCreation(path);
		xmlService.serialize(users.get(), path);
	}
	
	/**
	 * Load Users
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = "/users/load", method = RequestMethod.POST)
	public @ResponseBody void loadUsers() throws IOException {	
		
		final Path path = config.USERS.resolve("users.xml");
		final Collection<User> loadedUsers =  xmlService.deserialize(path, User.class);
		for(final User user : loadedUsers) {
			user.makeUnique();
		}
		data.removeAll(User.class);
		data.addAll(loadedUsers);
	}
	
	/**
	 * Enable/Disable User
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = {"/users/switch/{idLink}"}, method = RequestMethod.POST)
	public @ResponseBody void switchUser(@PathVariable("idLink") String idLink) {
		
		final User user = users.getByIdLink(idLink);
		user.enable(!user.isEnabled());
	}
}
