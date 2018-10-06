package gmm.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.service.assets.vcs.VcsPlugin;
import gmm.service.assets.vcs.VcsPluginSelector.ConditionalOnConfigSelector;

@Controller
@ConditionalOnConfigSelector("svn")
public class SvnController {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final VcsPlugin svn;
	
	@Autowired
	public SvnController(VcsPlugin vcs) {
		this.svn = vcs;
	}
	
	@Value("${vcs.notify.token}")
	private String configToken;
	
	@RequestMapping(value="${vcs.notify.url}", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> notifyCommit(
			@RequestParam(value="token", required=false) String token) {
		
		if (token == null || token.equals("") || !token.equals(configToken)) {
			logger.warn("Invalid commit notification received (no or wrong token).");
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		} else {
			logger.info("Valid commit notification received.");
			svn.notifyRepoChange();
			return new ResponseEntity<>(HttpStatus.OK);
		}
	}
}
