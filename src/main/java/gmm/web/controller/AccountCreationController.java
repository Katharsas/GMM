package gmm.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import gmm.domain.User;
import gmm.service.users.UserService;

@RequestMapping("newaccount")
@Controller
public class AccountCreationController {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final UserService userService;
	private final String accountToken;
	
	private AccountCreationController(UserService userService,
			@Value("${accountcreation.token}") String accountToken) {
		
		this.userService = userService;
		if (accountToken == null || accountToken.equals("")) {
			logger.warn("Configuration problem: accountcreation.token is missing or empty! Any attempts to use this token will fail.");
		}
		this.accountToken = accountToken;
	}
	
	/**
	 * Default Handler <br>
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String send(
			ModelMap model,
			HttpServletRequest request,
			HttpServletResponse response) {
		
		return "account";
	}
	
	@RequestMapping(value = "/create" , method = RequestMethod.POST)
	public String createAccount(
				@RequestParam String username,
				@RequestParam String password,
				@RequestParam(required=false) String token
			) {
		
		if (token == null || token.equals("") || !token.equals(accountToken)) {
			logger.debug("Invalid account creation submitted by user (no or wrong token).");
			return "redirect:/newaccount?wrongToken";
		} else {
			logger.debug("Valid account creation submitted by user.");
			if (!userService.isFreeUserName(username)) {
				return "redirect:/newaccount?nameTaken";
			} else if (password.length() < 8) {
				return "redirect:/newaccount?passwordTooShort";
			} else {
				final User user = new User(username);
				user.setPasswordHash(userService.encodePassword(password));
				user.setRole(User.ROLE_USER);
				user.enable(true);
				userService.add(user);
			}
			return "redirect:/login?newaccount";
		}
	}
}
