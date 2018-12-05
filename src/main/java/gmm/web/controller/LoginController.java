package gmm.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
	
	@RequestMapping(value="/", method = RequestMethod.GET)
	public String defaultPage() {
//		return new RedirectView("/tasks", true, false);
		return "redirect:/tasks";
	}
	
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String login(ModelMap model,
			@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "logout", required = false) String logout) {

		model.addAttribute("error", error != null);
		model.addAttribute("logout", logout != null);
		return "login";
	}
}
