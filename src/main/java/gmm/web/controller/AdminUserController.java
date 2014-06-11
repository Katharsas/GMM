package gmm.web.controller;

import gmm.collections.Collection;
import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.data.XMLService;
import gmm.web.AjaxResponseException;
import gmm.web.sessions.TaskSession;

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
			@RequestParam(value="role", required=false) String role) throws AjaxResponseException {
		try {
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
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Enable/Disable User Admin Role
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = {"/users/admin/{idLink}"}, method = RequestMethod.POST)
	public @ResponseBody void switchAdmin(@PathVariable("idLink") String idLink) throws AjaxResponseException {
		try {
			User user = users.getByIdLink(idLink);
			user.setRole(user.getRole().equals(User.ROLE_ADMIN) ? 
					User.ROLE_USER : User.ROLE_ADMIN);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Generate User Password
	 * -----------------------------------------------------------------
	 * This resets the users password to a new randomly generated password.
	 * @throws AjaxResponseException 
	 */
	@RequestMapping(value = "/users/reset/{idLink}")
	public @ResponseBody String resetPassword(@PathVariable("idLink") String idLink) throws AjaxResponseException {
		try {
			User user = users.getByIdLink(idLink);
			String password = users.generatePassword();
			user.setPasswordHash(encoder.encode(password));
			return password;
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Save Users
	 * -----------------------------------------------------------------
	 * @throws AjaxResponseException 
	 */
	@RequestMapping(value = "/users/save", method = RequestMethod.POST)
	public @ResponseBody void saveUsers() throws AjaxResponseException {
		try {
			Path path = config.USERS.resolve("users.xml");
			fileService.prepareFileCreation(path);
			xmlService.serialize(users.get(), path);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Load Users
	 * -----------------------------------------------------------------
	 * @throws AjaxResponseException 
	 */
	@RequestMapping(value = "/users/load", method = RequestMethod.POST)
	public @ResponseBody void loadUsers() throws AjaxResponseException {	
		try {
			Path path = config.USERS.resolve("users.xml");
			Collection<? extends User> loadedUsers =  xmlService.deserialize(path, User.class);
			for(User user : loadedUsers) {
				user.makeUnique();
			}
			data.removeAll(User.class);
			data.addAll(User.class, loadedUsers);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Enable/Disable User
	 * -----------------------------------------------------------------
	 * @throws AjaxResponseException 
	 */
	@RequestMapping(value = {"/users/switch/{idLink}"}, method = RequestMethod.POST)
	public @ResponseBody void switchUser(@PathVariable("idLink") String idLink) throws AjaxResponseException {
		try {
			User user = users.getByIdLink(idLink);
			user.enable(!user.isEnabled());
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
}
