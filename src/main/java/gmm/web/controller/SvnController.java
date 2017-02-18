package gmm.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import gmm.service.data.vcs.SvnPlugin;

@Controller
public class SvnController {

	private final SvnPlugin svn;
	
	@Autowired
	public SvnController(SvnPlugin svn) {
		this.svn = svn;
	}
	
	@Value("${vcs.plugin.svn.token}")
	private String configToken;
	
	@RequestMapping(value="/svn/notifyCommit", method = RequestMethod.POST)
	public void notifyCommit(
			@RequestParam(value="token", required=false) String token) {
		
		if (token == null || !token.equals(configToken)) {
			throw new AccessDeniedException("");
		} else {
			svn.onCommitHookNotified();
		}
	}
}
