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
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnDiffStatus;
import org.tmatesoft.svn.core.wc2.SvnDiffSummarize;
import org.tmatesoft.svn.core.wc2.SvnGetInfo;
import org.tmatesoft.svn.core.wc2.SvnGetStatus;
import org.tmatesoft.svn.core.wc2.SvnInfo;
import org.tmatesoft.svn.core.wc2.SvnOperation;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnReceivingOperation;
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

@Service
@ConditionalOnConfigSelector("svn")
public class SvnPlugin extends VcsPlugin {
	
	public static class UncheckedSVNExeption extends RuntimeException {
		private static final long serialVersionUID = 1154711366260343957L;

		public UncheckedSVNExeption(SVNException cause) {
			super(cause);
		}
		
		public UncheckedSVNExeption(String message, SVNException cause) {
			super(message, cause);
		}
		
		@Override
		public synchronized SVNException getCause() {
			return (SVNException) super.getCause();
		}
	}
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final DataConfigService config;
	private final FileService fileService;
	
	private final SvnTarget repository;
	private final SvnTarget workingCopy;
	
	private final SvnOperationFactory svnOperationFactory;
	
	@Autowired
	public SvnPlugin(DataConfigService config, FileService fileService,
			@Value("${vcs.plugin.svn.repository}") String repositoryUriString) {
		
		this.config = config;
		this.fileService = fileService;
		
		svnOperationFactory = new SvnOperationFactory();
		
		try {
			final URI uri = new URI(repositoryUriString);
			final SVNURL url = SVNURL.parseURIEncoded(uri.toASCIIString());
			repository = SvnTarget.fromURL(url, SVNRevision.HEAD);
		} catch (URISyntaxException | SVNException e) {
			throw new IllegalArgumentException("Invalid SVN repository uri!", e);
		}
		
		checkRepoAvailable();
		
		final File workingCopyDir = config.assetsNew().toFile();
		workingCopy = SvnTarget.fromFile(workingCopyDir, SVNRevision.WORKING);
		
		initializeWorkingCopy(workingCopyDir);
	}
	
	@Override
	public void init() {
		final List<Path> changedPaths = diffAndUpdate();
		notifyFilesChanged(changedPaths);
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
	 * @return Current revision of remote repository.
	 */
	private long checkRepoAvailable() {
		final SvnGetInfo operation = svnOperationFactory.createGetInfo();
		operation.setSingleTarget(repository);
		try {
			final SvnInfo info = operation.run();
			logger.info("Successfully reached SVN repository at uri '" + repository.getPathOrUrlDecodedString() + "'.");
			return info.getRevision();
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
			// if logger is not in debug mode, SvnStatus will not list files of status 'normal',
			// so the list of statuses will be empty if all files have status 'normal'
			
			final SVNStatusType type = entry.getNodeStatus();
			final String filePath = entry.getPath().getPath();
			
			if (type == SVNStatusType.STATUS_NORMAL) {
				if (logger.isDebugEnabled()) {
					logger.debug("Found versioned working copy entry (status normal): '" + filePath +"'");
				}
			} else {
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
	
	private List<Path> diffAndUpdate() {
		final long repositoryRev = retrieveRevision(repository);
		final long workingCopyRev = retrieveRevision(workingCopy);
		
		if (workingCopyRev > repositoryRev) {
			throw new IllegalStateException("Repository cannot have older revision than working copy!");
		} else if (workingCopyRev < repositoryRev) {
			logger.info("Executing working copy update.");
			
			final SVNRevision oldRevision = SVNRevision.create(workingCopyRev);
			final SVNRevision newRevision = SVNRevision.create(repositoryRev);
			
			final SvnDiffSummarize operation = svnOperationFactory.createDiffSummarize();
			operation.setSource(repository, oldRevision, newRevision);
			
			final List<SvnDiffStatus> result = new ArrayList<>(SvnDiffStatus.class);
			tryRun(operation, result);
			
			final List<Path> changedPaths = new ArrayList<>(Path.class, result.size());
			
			for (final SvnDiffStatus status : result) {
				if (status.getFile().isFile()) {
					logger.debug("Found modified file. Path: '" + status.getPath() + "' Status: '" + status.getModificationType() + "'");
					changedPaths.add(Paths.get(status.getPath()));
				}
			}
			
			final SvnUpdate update = svnOperationFactory.createUpdate();
			update.setSingleTarget(workingCopy);
			tryRun(update);
			
			return changedPaths;
		} else {
			logger.info("Skipping working copy update since it is already at remote HEAD revision.");
			return new ArrayList<>(Path.class, 0);
		}
	}
	
	private <V, T extends SvnOperation<V>> V tryRun(T operation) {
		try {
			return operation.run();
		} catch (final SVNException e) {
			throw new UncheckedSVNExeption(e);
		}
	}
	
	private <V, T extends SvnReceivingOperation<V>> Collection<V> tryRun(T operation, Collection<V> receiver) {
		try {
			return (Collection<V>) operation.run(receiver);
		} catch (final SVNException e) {
			throw new UncheckedSVNExeption(e);
		}
	}
	
	@PreDestroy
	protected void close() {
		svnOperationFactory.dispose();
	}
	
	public synchronized void onCommitHookNotified() {
		final List<Path> changedPaths = diffAndUpdate();
		if (changedPaths.size() > 0) {
			notifyFilesChanged(changedPaths);
		}
	}

	@Override
	public boolean isCustomAssetPathsAllowed() {
		return true;
	}

	

	@Override
	public void commitAddedFile(Path file) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void commitChangedFile(Path file) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void commitRemovedFile(Path file) {
		// TODO Auto-generated method stub
		
	}
}
