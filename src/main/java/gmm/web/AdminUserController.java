package gmm.web;

import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.data.XMLService;
import gmm.util.Collection;
import gmm.web.sessions.TaskSession;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
			@RequestParam(value="role", required=false) String role)
	{
		if(idLink.equals("new")) {
			User user = new User(name);
			data.add(user);
		}
		else {
			User user = users.getByIdLink(idLink);
			if(name != null) user.setName(name);
			if(role != null) user.setRole(role);
		}
	}
	
	/**
	 * Enable/Disable User Admin Role
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = {"/users/admin/{idLink}"}, method = RequestMethod.POST)
	public @ResponseBody void switchAdmin(@PathVariable("idLink") String idLink)
	{
		User user = users.getByIdLink(idLink);
		user.setRole(user.getRole().equals(User.ROLE_ADMIN) ? 
				User.ROLE_USER : User.ROLE_ADMIN);
	}
	
	/**
	 * Generate User Password
	 * -----------------------------------------------------------------
	 * This resets the users password to a new randomly generated password.
	 */
	@RequestMapping(value = "/users/reset/{idLink}")
	public @ResponseBody String resetPassword(@PathVariable("idLink") String idLink)
	{
		User user = users.getByIdLink(idLink);
		String password = users.generatePassword();
		user.setPasswordHash(encoder.encode(password));
		return password;
	}
	
	/**
	 * Save Users
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = "/users/save", method = RequestMethod.POST)
	public @ResponseBody void saveUsers() throws IOException
	{
		Path path = Paths.get(config.DATA_USERS).resolve("users.xml");
		fileService.prepareFileCreation(path);
		xmlService.serialize(users.get(), path);
	}
	
	/**
	 * Load Users
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = "/users/load", method = RequestMethod.POST)
	public @ResponseBody void loadUsers() throws IOException
	{	
		Path path = Paths.get(config.DATA_USERS).resolve("users.xml");
		data.removeAll(User.class);
		Collection<? extends User> loadedUsers =  xmlService.deserialize(path, User.class);
		for(User user : loadedUsers) {
			user.makeUnique();
		}
		data.addAll(User.class, loadedUsers);
	}
	
	/**
	 * Enable/Disable User
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = {"/users/switch/{idLink}"}, method = RequestMethod.POST)
	public @ResponseBody void switchUser(@PathVariable("idLink") String idLink)
	{	
		User user = users.getByIdLink(idLink);
		user.enable(!user.isEnabled());
	}
}
