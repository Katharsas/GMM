package gmm.service.assets.vcs;

import java.io.File;
import java.nio.file.Path;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.svnkit.SvnKitClientAdapterFactory;

import gmm.collections.ArrayList;
import gmm.collections.List;
import gmm.service.FileService;
import gmm.service.assets.vcs.SvnPlugin.UncheckedSVNExeption;
import gmm.service.assets.vcs.VcsPluginSelector.ConditionalOnConfigSelector;
import gmm.service.data.DataConfigService;

/**
 * Plugin using the SVN library used by Subclipse, underlying implementation can be swapped out:
 * - JavaHL
 * - SVNKit java implementation
 * - Command Line
 * 
 * See https://github.com/subclipse/svnclientadapter
 * Samples: https://github.com/subclipse/svnclientadapter/tree/master/samples
 * 
 * This could one day replace the SVNKit high level lib. The lib seems to be easier/clearer, and
 * since it should be tried and tested it should not be too buggy.
 * 
 * @author Jan Mothes
 */
@Service
@ConditionalOnConfigSelector("svn")
public class SvnPlugin2 {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private Path workingCopy;
	
	private ISVNClientAdapter svnClient;
	
	private boolean logSuperFineDebug = false;
	
	private boolean running = false;
	
	private List<Path> updatedPaths;
	
	@Autowired
	public SvnPlugin2(DataConfigService config, FileService fileService,
			@Value("${vcs.plugin.svn.repository}") String repositoryUriString,
			@Value("${vcs.plugin.svn.username}") String repositoryUsername,
			@Value("${vcs.plugin.svn.password}") String repositoryPassword
			) {
		
		this.workingCopy = config.assetsNew();
	}
	
	@PostConstruct
	public void init() {
		try {
//			JhlClientAdapterFactory.setup();
			SvnKitClientAdapterFactory.setup();
//			CmdLineClientAdapterFactory.setup();
			
			final String bestClientType = SVNClientAdapterFactory.getPreferredSVNClientType();
			logger.debug("Using "+ bestClientType +" factory");
			
			svnClient = SVNClientAdapterFactory.createSVNClient(bestClientType);
			svnClient.addNotifyListener(new NotifyListener());
			
		} catch (final SVNClientException e) {
			throw new UncheckedSVNExeption(e);
		}
	}

	public List<Path> doUpdate() {
		try {
			updatedPaths = new ArrayList<>(Path.class);
			running = true;
			if (logSuperFineDebug) {
				logger.debug(" - \n");
			}
			svnClient.update(workingCopy.toFile(), SVNRevision.HEAD, true);
			while (running) {
				try {
					Thread.sleep(10);
				} catch (final InterruptedException e) {}
			}
			return updatedPaths;
		} catch (final SVNClientException e) {
			throw new UncheckedSVNExeption(e);
		}
	}
	
	public class NotifyListener implements ISVNNotifyListener {

		@Override
		public void logCommandLine(String message) {
			if (logSuperFineDebug) {
				logger.debug(message);
			}
		}

		@Override
		public void logCompleted(String message) {
			if (logSuperFineDebug) {
				logger.debug(message);
			}
			running = false;
		}

		@Override
		public void logError(String message) {
			if (logSuperFineDebug) {
				logger.debug(message);
			}
		}

		@Override
		public void logMessage(String message) {
			if (logSuperFineDebug) {
				logger.debug(message);
			}
		}

		@Override
		public void logRevision(long revision, String path) {
			if (logSuperFineDebug) {
				logger.debug("revision :" +revision);
			}
		}

		@Override
		public void onNotify(File path, SVNNodeKind nodeKind) {
			updatedPaths.add(workingCopy.relativize(path.toPath()));
		}

		@Override
		public void setCommand(int cmd) {
		}
	}
}
