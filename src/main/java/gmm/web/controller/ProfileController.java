package gmm.web.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.service.users.UserService;


@Controller
@RequestMapping("profile")
@PreAuthorize("hasRole('ROLE_USER')")

public class ProfileController {

	private final UserService users;
	
	@Autowired
	public ProfileController(UserService users) {
		this.users = users;
	}
	
	@RequestMapping(method = RequestMethod.GET)
    public String send(ModelMap model) {
		
        return "profile";
    }
	
	@RequestMapping(value = "/password", method = RequestMethod.POST)
	public @ResponseBody PasswordChangeResult changePassword(
			@RequestParam("oldPW") String oldPw,
			@RequestParam("newPW") String newPw) {
		
		if(users.matchesPassword(oldPw, users.getLoggedInUser())) {
			if(newPw.length() < 8) {
				return new PasswordChangeResult("Error: Password too short!");
			}
			users.getLoggedInUser().setPasswordHash(users.encodePassword(newPw));
			return new PasswordChangeResult(null);
		}
		else {
			return new PasswordChangeResult("Error: Wrong current password!");
		}
	}
	
	protected static class PasswordChangeResult {
		public String error;
		public PasswordChangeResult(String error) {
			this.error = error;
		}
	}
}
