package gmm.web;

import gmm.service.UserService;
import gmm.web.sessions.TaskSession;

import org.springframework.ui.ModelMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("profile")
@PreAuthorize("hasRole('ROLE_USER')")

public class ProfileController {

	@Autowired TaskSession session;
	@Autowired UserService userService;
	@Autowired PasswordEncoder encoder;
	
	@RequestMapping(method = RequestMethod.GET)
    public String send(ModelMap model) {
		
        return "profile";
    }
	
	@RequestMapping(value = "/password", method = RequestMethod.POST)
	public @ResponseBody String changePassword(
			@RequestParam("oldPW") String oldPW,
			@RequestParam("newPW") String newPW) {
		
		if(encoder.matches(oldPW, session.getUser().getPasswordHash())) {
			if(newPW.length()<8) {
				return "Error: Password too short!";
			}
			newPW = encoder.encode(newPW);
			session.getUser().setPasswordHash(newPW);
			return "";
		}
		else {
			return "Error: Wrong current password!";
		}
	}
}
