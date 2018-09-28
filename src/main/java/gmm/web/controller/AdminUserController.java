package gmm.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.domain.User;
import gmm.domain.User.UserNameOccupiedException;
import gmm.service.data.DataAccess;
import gmm.service.data.backup.BackupExecutorService;
import gmm.service.users.UserService;

@Controller
@RequestMapping("admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminUserController {
	
	private final DataAccess data;
	private final UserService users;
	private final BackupExecutorService backups;
	
	@Autowired
	public AdminUserController(DataAccess data, UserService users, BackupExecutorService backups) {
		this.data = data;
		this.users = users;
		this.backups = backups;
	}
	
	/**
	 * Edit User
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = {"/users/edit/{idLink}"}, method = RequestMethod.POST)
	public @ResponseBody void editUser(
			@PathVariable("idLink") String idLink,
			@RequestParam(value="name", required=false) String name,
			@RequestParam(value="role", required=false) String role) {
		
		final boolean isFreeName = users.isFreeUserName(name);
		final String existsMessage = "Another user with name '" + name + "' already exists!";
		final boolean isNew = idLink.equals("new");
		if (!isFreeName) {
			final String message = (isNew ? "Can't add user. " : "Can't change name. ") + existsMessage;
			throw new UserNameOccupiedException(name, message);
		}
		if(isNew) {
			final User newUser = new User(name);
			if(role != null) newUser.setRole(role);
			data.add(newUser);
		} else {
			final User user = users.getByIdLink(idLink);
			if(name != null) {
				user.setName(name);
			}
			if(role != null) user.setRole(role);
			// TODO edit event?!
			// any changes to a user should always synchronize on user!
			// if users were immutable, this would not be a problem, and all tasks would have to be updated.
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
		// TODO edit event?!
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
		user.setPasswordHash(users.encodePassword(password));
		return new String[] {password};
		// TODO edit event?!
	}
	
	/**
	 * Trigger User Backup
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = "/users/save", method = RequestMethod.POST)
	public @ResponseBody void saveUsers() {
		backups.triggerUserBackup();
	}
	
	/**
	 * Enable/Disable User
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = {"/users/switch/{idLink}"}, method = RequestMethod.POST)
	public @ResponseBody void switchUser(@PathVariable("idLink") String idLink) {
		
		final User user = users.getByIdLink(idLink);
		user.enable(!user.isEnabled());
		// TODO edit event?!
	}
}
