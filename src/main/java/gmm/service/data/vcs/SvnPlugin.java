package gmm.service.data.vcs;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnGetInfo;
import org.tmatesoft.svn.core.wc2.SvnInfo;
import org.tmatesoft.svn.core.wc2.SvnOperation;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import gmm.ConfigurationException;
import gmm.service.FileService;
import gmm.service.data.DataConfigService;
import gmm.service.data.vcs.VcsPluginSelector.ConditionalOnConfigSelector;

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
	
	private long workingCopyRevision = 0;
	
	final SvnOperationFactory svnOperationFactory;
	
	@Autowired
	public SvnPlugin(DataConfigService config, FileService fileService,
			@Value("${vcs.plugin.svn.repository}") String repositoryUriString) {
		
		super(config);
		this.config = config;
		this.fileService = fileService;
		
		svnOperationFactory = new SvnOperationFactory();
		
		try {
			final URI repositoryUri = new URI(repositoryUriString);
			repository = SvnTarget.fromURL(SVNURL.parseURIEncoded(repositoryUri.toASCIIString()));
		} catch (URISyntaxException | SVNException e) {
			throw new IllegalArgumentException("Invalid SVN repository uri!", e);
		}
		
		final long repositoryRev = checkRepoAvailable();
		
		final File workingCopyFile = config.assetsNew().toFile();
		workingCopy = SvnTarget.fromFile(workingCopyFile);
		
		final long workingCopyRev = initializeWorkingCopy(workingCopyFile);
		
		workingCopyRevision = workingCopyRev;
		diffAndUpdate(repositoryRev, workingCopyRev);
		
		// TODO the gmm should probably save his previews NOT in the wc, because
		// 1. SVN does not ignore empty folders like git does
		// 2. Original Preview generation will cause the GMM to generate folders for all original assets in wc.
		//    Now what happens when a user wants to save an new asset under a path thats not the same as for the original?
		//    That should totally work but will create conflicts / GMM needs to move previews into new location => complicated
		// => GMM should have a preview store whith flat list of asset_folder / previews files, which can be mapped to tasks, as well as (different) original and new paths
	}
	
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
	 * @return Current revision of the working copy.
	 */
	private long initializeWorkingCopy(File workingCopyFile) {
		
		final String workingCopyString = workingCopyFile.toString();
		final long result;
		
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
					
					// TODO check doStatus to see if the given working copy is in usable state (no pending changes)
					
					result = info.getRevision();
				} else {
					logger.warn("Could not locate working copy at path '" + workingCopyString + "'!");
					if(workingCopyFile.list().length > 0) {
						throw new ConfigurationException("Cannot create new working copy because directory is not empty! Path: '" + workingCopyString + "'");
					}
					result = createNewWorkingCopy();
				}
			} else {
				throw new ConfigurationException("Path to working copy is not a directory! Path:'" + workingCopyString + "'");
			}
		} else {
			logger.warn("Could not locate SVN working copy at path '" + workingCopyString + "'!");
			fileService.createDirectory(workingCopyFile.toPath());
			result = createNewWorkingCopy();
		}
		return result;
	}
	
	private long createNewWorkingCopy() {
		logger.info("Creating new SVN working copy at path '" + workingCopy.getPathOrUrlDecodedString() + "'.");
		
		final SvnCheckout operation = svnOperationFactory.createCheckout();
	    operation.setSingleTarget(workingCopy);
	    operation.setSource(repository);
	    
	    return tryRun(operation);
	}
	
	private void diffAndUpdate(long repositoryRev, long workingCopyRev) {
		if (workingCopyRev > repositoryRev) {
			throw new IllegalStateException("Repository cannot have older revision than working copy!");
		} else if (workingCopyRev < repositoryRev) {
			// TODO update
		}
	}
	
//	private <T extends SvnOperation<?>> T setSingleTarget(T operation) {
//		operation.setSingleTarget(workingCopy);
//		return operation;
//	}
	
	private <V, T extends SvnOperation<V>> V tryRun(T operation) {
		try {
			return operation.run();
		} catch (final SVNException e) {
			throw new UncheckedSVNExeption(e);
		}
	}
	
	@PreDestroy
	protected void close() {
		svnOperationFactory.dispose();
	}
	
	public synchronized void onCommitHookNotified() {
		
	}

	@Override
	public boolean allowCustomAssetPaths() {
		return true;
	}
}
