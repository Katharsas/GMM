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

import gmm.service.assets.vcs.SvnPlugin;
import gmm.service.assets.vcs.VcsPluginSelector.ConditionalOnConfigSelector;

@Controller
@ConditionalOnConfigSelector("svn")
public class SvnController {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final SvnPlugin svn;
	
	@Autowired
	public SvnController(SvnPlugin svn) {
		this.svn = svn;
	}
	
	@Value("${vcs.plugin.svn.token}")
	private String configToken;
	
	@RequestMapping(value="/plugins/svn/notifyCommit", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> notifyCommit(
			@RequestParam(value="token", required=false) String token) {
		
		if (token == null || !token.equals(configToken)) {
			logger.debug(token);
			logger.debug("Invalid commit notification received (no or wrong token).");
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		} else {
			logger.debug("Valid commit notification received.");
			svn.onCommitHookNotified();
			return new ResponseEntity<>(HttpStatus.OK);
		}
	}
}
