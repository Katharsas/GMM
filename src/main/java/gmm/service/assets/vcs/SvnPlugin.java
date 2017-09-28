package gmm.service.assets.vcs;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnCommit;
import org.tmatesoft.svn.core.wc2.SvnDiffStatus;
import org.tmatesoft.svn.core.wc2.SvnDiffSummarize;
import org.tmatesoft.svn.core.wc2.SvnGetInfo;
import org.tmatesoft.svn.core.wc2.SvnGetStatus;
import org.tmatesoft.svn.core.wc2.SvnInfo;
import org.tmatesoft.svn.core.wc2.SvnOperation;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnReceivingOperation;
import org.tmatesoft.svn.core.wc2.SvnScheduleForAddition;
import org.tmatesoft.svn.core.wc2.SvnScheduleForRemoval;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpdate;

import gmm.ConfigurationException;
import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.List;
import gmm.service.FileService;
import gmm.service.assets.vcs.VcsPluginSelector.ConditionalOnConfigSelector;
import gmm.service.data.DataConfigService;

/**
 * SVN client implementation using SVNKit new high-level interface (org.tmatesoft.svn.core.wc2.*).
 * Documentation for this SVNKit interface is only available as JavaDoc and StackOverflow answers.
 * 
 * Under the hood, SVNKit uses a Java re-implementation of the native SVN client. It seems like this
 * re-implementation provides a low level client interface which matches the native interfaces, so
 * in theory it should be possible to run the high level SVN API on top of the native API (JavaHL)
 * by witching out the low level implementation.
 * It's unclear though, which dependency would need to be exchanged for SVNKit to use JavaHL instead
 * of its own implementation, or if its even possible to do so.
 * 
 * Bug: https://issues.tmatesoft.com/issue/SVNKIT-708 (workaround implemented)
 * 
 * @author Jan Mothes
 */
@Service
@ConditionalOnConfigSelector("svn")
public class SvnPlugin extends VcsPlugin {
	
	public static class UncheckedSVNExeption extends RuntimeException {
		private static final long serialVersionUID = 1154711366260343957L;

		public UncheckedSVNExeption(SVNClientException cause) {
			super(cause);
		}
		
		public UncheckedSVNExeption(String message, SVNClientException cause) {
			super(message, cause);
		}
		
		public UncheckedSVNExeption(SVNException cause) {
			super(cause);
		}
		
		public UncheckedSVNExeption(String message, SVNException cause) {
			super(message, cause);
		}
		
		@Override
		public synchronized Exception getCause() {
			return (Exception) super.getCause();
		}
	}
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final FileService fileService;
	
	private final Path workingCopyDir;
	
	private final SvnTarget repository;
	private final SvnTarget workingCopy;
	
	private final SvnOperationFactory svnOperationFactory;
	
	@Autowired
	public SvnPlugin(DataConfigService config, FileService fileService,
			@Value("${vcs.plugin.svn.repository}") String repositoryUriString,
			@Value("${vcs.plugin.svn.username}") String repositoryUsername,
			@Value("${vcs.plugin.svn.password}") String repositoryPassword
			) {
		
		this.fileService = fileService;
		
		svnOperationFactory = new SvnOperationFactory();
		if (repositoryUsername != null && repositoryUsername != "") {
			svnOperationFactory.setAuthenticationManager(
					BasicAuthenticationManager.newInstance(repositoryUsername, repositoryPassword.toCharArray()));
		}
		
		try {
			final String uriString = repositoryUriString.replaceAll(" ", "%20");
			final URI uri = new URI(uriString);
			final SVNURL url = SVNURL.parseURIEncoded(uri.toASCIIString());
			repository = SvnTarget.fromURL(url, SVNRevision.HEAD);
			
		} catch (URISyntaxException | SVNException e) {
			throw new IllegalArgumentException("Invalid SVN repository uri!", e);
		}
		checkRepoAvailable();
		
		workingCopyDir = config.assetsNew();
		workingCopy = SvnTarget.fromFile(workingCopyDir.toFile(), SVNRevision.WORKING);
		
		initializeWorkingCopy(workingCopyDir.toFile());
	}

	@Override
	public void init() {
		final List<Path> changedPaths = diffAndUpdate();
		onFilesChanged(changedPaths);
	}
	
	/**
	 * @return Current revision of remote repository or working copy.
	 */
	private long retrieveRevision(SvnTarget target) {
		final SvnGetInfo operation = svnOperationFactory.createGetInfo();
		operation.setSingleTarget(target);
		final SvnInfo info = tryRun(operation);
		return info.getRevision();
	}
	
	/**
	 * @return Root URL of the SVN repo as opposed to the checked out URL.
	 */
	private SVNURL checkRepoAvailable() {
		final SvnGetInfo operation = svnOperationFactory.createGetInfo();
		operation.setSingleTarget(repository);
		try {
			final SvnInfo info = operation.run();
			logger.info("Successfully reached SVN repository at uri '" + repository.getPathOrUrlDecodedString() + "'.");
			return info.getRepositoryRootUrl();
		} catch (final SVNException e) {
			logger.error("Could not reach SVN repository at uri '" + repository.getPathOrUrlDecodedString() + "'!", e);
			throw new UncheckedSVNExeption(e);
		}
	}
	
	/**
	 * @return Current revision of local working copy.
	 */
	private void initializeWorkingCopy(File workingCopyFile) {
		
		final String workingCopyString = workingCopyFile.toString();
		
		if (workingCopyFile.exists()) {
			if (workingCopyFile.isDirectory()) {
				final SvnGetInfo operation = svnOperationFactory.createGetInfo();
				operation.setSingleTarget(workingCopy);
				SvnInfo info = null;
				try {
					info = operation.run();
				} catch (final SVNException e) {}
				if (info != null) {
					logger.info("Located existing working copy at path '" + workingCopyString + "'.");
					verifyExistingWorkingCopy();
				} else {
					logger.warn("Could not locate working copy at path '" + workingCopyString + "'! Creating new one.");
					if(workingCopyFile.list().length > 0) {
						throw new ConfigurationException("Cannot create new working copy because directory is not empty! Path: '" + workingCopyString + "'");
					}
					createNewWorkingCopy();
				}
			} else {
				throw new ConfigurationException("Path to working copy is not a directory! Path:'" + workingCopyString + "'");
			}
		} else {
			logger.warn("Could not locate SVN working copy at path '" + workingCopyString + "'! Creating new one.");
			fileService.createDirectory(workingCopyFile.toPath());
			createNewWorkingCopy();
		}
	}
	
	private void verifyExistingWorkingCopy() {
		final SvnGetStatus ops = svnOperationFactory.createGetStatus();
		ops.setSingleTarget(workingCopy);
		ops.setReportAll(logger.isDebugEnabled());
		final Collection<SvnStatus> statuses = tryRun(ops, new ArrayList<>(SvnStatus.class));
		
		for (final SvnStatus entry : statuses) {
			
			final SVNStatusType type = entry.getNodeStatus();
			final String filePath = entry.getPath().getPath();
			
			if (type != SVNStatusType.STATUS_NORMAL) {
				logger.error("Existing working copy is in an invalid state (maybe modified)! "
						+ "Pls fix manually or delete working copy at '" + workingCopy + "'.");
				throw new ConfigurationException(
						"Found entry with invalid status in working copy! "
						+ "Relative path: '" + filePath +"'. Status: '" + type + "'.");
			}
		}
	}
	
	private long createNewWorkingCopy() {
		logger.info("Creating new SVN working copy at path '" + workingCopy.getPathOrUrlDecodedString() + "'.");
		
		final SvnCheckout operation = svnOperationFactory.createCheckout();
	    operation.setSingleTarget(workingCopy);
	    operation.setSource(repository);
	    
	    return tryRun(operation);
	}
	
	/**
	 * @return see {@link #diff(SVNRevision, SVNRevision)}
	 */
	private List<Path> diffAndUpdate() {
		final long repositoryRev = retrieveRevision(repository);
		final long workingCopyRev = retrieveRevision(workingCopy);
		
		if (workingCopyRev > repositoryRev) {
			throw new IllegalStateException("Repository cannot have older revision than working copy!");
		} else if (workingCopyRev < repositoryRev) {
			logger.info("Executing working copy update.");
			
			final SVNRevision oldRevision = SVNRevision.create(workingCopyRev);
			final SVNRevision newRevision = SVNRevision.create(repositoryRev);
			
			final SvnUpdate update = svnOperationFactory.createUpdate();
			update.setSingleTarget(workingCopy);
			tryRun(update);
			
			return diff(oldRevision, newRevision);
			
		} else {
			logger.info("Skipping working copy update since it is already at remote HEAD revision.");
			return new ArrayList<>(Path.class, 0);
		}
	}
	
	/**
	 * @return For some reason, added empty directories (like AssetFolders) are not detected, but
	 * 		deleted ones are ! (TODO fix?)
	 */
	private List<Path> diff(SVNRevision oldRevision, SVNRevision newRevision) {
		
		final SvnDiffSummarize operation = svnOperationFactory.createDiffSummarize();
		operation.setSource(workingCopy, oldRevision, newRevision);
		
		final List<SvnDiffStatus> result = new ArrayList<>(SvnDiffStatus.class);
		tryRun(operation, result);
		
		final List<Path> changedPaths = new ArrayList<>(Path.class, result.size());
		
		for (final SvnDiffStatus status : result) {
			/* Do not use status.getPath() or status.getFile(), they are unreliable! */
			
			final String url = status.getUrl().getPath();
			final String repo = repository.getURL().getPath();
			if (!url.startsWith(repo)) {
				throw new IllegalStateException();
			}
			String relative = url.substring(repo.length());
			if (relative.startsWith("/")) relative = relative.substring(1);
			
			final Path relPath = Paths.get(relative);
			
			logger.debug("Found modified file. Path: '" + relPath + "' Status: '" + status.getModificationType() + "'");
			changedPaths.add(relPath.normalize());
		}
		return changedPaths;
	}
	
	private <V, T extends SvnOperation<V>> V tryRun(T operation) {
		try {
			logger.debug("Running: " + operation .getClass().getSimpleName());
			return operation.run();
		} catch (final SVNException e) {
			throw new UncheckedSVNExeption(e);
		}
	}
	
	private <V, T extends SvnReceivingOperation<V>> Collection<V> tryRun(T operation, Collection<V> receiver) {
		try {
			logger.debug("Running: " + operation .getClass().getSimpleName());
			return (Collection<V>) operation.run(receiver);
		} catch (final SVNException e) {
			throw new UncheckedSVNExeption(e);
		}
	}
	
	@PreDestroy
	protected void close() {
		svnOperationFactory.dispose();
	}

	@Override
	public synchronized void notifyRepoChange() {
		final List<Path> changedPaths = diffAndUpdate();
		onFilesChanged(changedPaths);
	}

	@Override
	public boolean isCustomAssetPathsAllowed() {
		return true;
	}

	

	@Override
	public void commitAddedFile(Path file) {
		final SvnScheduleForAddition ops = svnOperationFactory.createScheduleForAddition();
		final Path abs = workingCopyDir.resolve(file);
		ops.setSingleTarget(SvnTarget.fromFile(abs.toFile()));
		ops.setAddParents(true);
		tryRun(ops);

		commit("GMM: Added file.");
	}

	@Override
	public void commitChangedFile(Path file) {
		// TODO check if change is committed
		
		commit("GMM: Replaced file.");
	}

	@Override
	public void commitRemovedFile(Path file) {
		final SvnScheduleForRemoval ops = svnOperationFactory.createScheduleForRemoval();
		final Path abs = workingCopyDir.resolve(file);
		ops.setSingleTarget(SvnTarget.fromFile(abs.toFile()));
		tryRun(ops);
		
		commit("GMM: Deleted file.");
	}

	private void commit(String message) {
		final SvnCommit ops = svnOperationFactory.createCommit();
	    ops.setSingleTarget(workingCopy);// only changes below this path will be committed
	    ops.setDepth(SVNDepth.INFINITY);// TODO needed or default?
	    ops.setCommitMessage(message);
	    tryRun(ops);
	}
}
