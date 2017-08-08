package gmm.service.assets.vcs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.joda.time.DateTime;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnCommit;
import org.tmatesoft.svn.core.wc2.SvnDiffStatus;
import org.tmatesoft.svn.core.wc2.SvnDiffSummarize;
import org.tmatesoft.svn.core.wc2.SvnGetInfo;
import org.tmatesoft.svn.core.wc2.SvnInfo;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnScheduleForAddition;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpdate;

import gmm.collections.ArrayList;
import gmm.collections.Collection;

public class SVNTest {
	
	final SvnTarget svnRepoRoot = SvnTarget.fromFile(new File("C:/SVNServer/trunk/project/newAssets"));
	
	final Path workingCopyPath = null;
	final SvnTarget workingCopy = SvnTarget.fromFile(new File("workspace/newAssets"));
	
	final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();//TODO call dispose always (finally) even on exception of any method
	
	long currentRevision;// TODO should be most recent one, when repo gets new revision, use method getChangedFilesSinceRevision to get file changes.
	
	// TODO when access to repository is not possible, any file uploads cannot be made and must be disabled.
	// TODO when access to repository is not possible, file downloads must be disabled because the files could be outdated and the GMM woudn't know that.
	
	public void init() throws SVNException {
		
		final ISVNAuthenticationManager authManager =
                   SVNWCUtil.createDefaultAuthenticationManager("(login name)", "(login password)".toCharArray());
		
		final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
		svnOperationFactory.setAuthenticationManager(authManager);
	}
	
	/**
	 * Checkout(SVN) / Clone(Git) to create a local working copy from repo.
	 * Only needed when there is no working copy yet.
	 */
	public void createWorkingCopy() throws SVNException {
		
	    final SvnCheckout checkout = svnOperationFactory.createCheckout();
	    checkout.setSingleTarget(workingCopy);
	    checkout.setSource(svnRepoRoot);
	    
	    final long revision = checkout.run();
	    svnOperationFactory.dispose();
	}
	
	/**
	 * Update(SVN) / Fetch(Git) local working copy with latest files from repo.
	 */
	public void updateWorkingCopy() throws SVNException {
		
		final SvnUpdate update = svnOperationFactory.createUpdate();
		update.setSingleTarget(workingCopy);
		
		final long[] revisions = update.run();
	}
	
	/**
	 * Find out at which revision number the repo is at.
	 * Can also be used on single files to find out in which revision the last changed occured to them.
	 */
	public void latestRevision() throws SVNException {
		
		final SvnGetInfo operation = svnOperationFactory.createGetInfo();
		operation.setSingleTarget(workingCopy);
		
		final SvnInfo info = operation.run();
		final long revision = info.getRevision();
	}
	
	/**
	 * Get all paths to all changed files since an older revision.
	 */
	public void getChangedFilesSinceRevision() throws SVNException {
		
		final SVNRevision oldRevision = SVNRevision.create(0);
		final SVNRevision latestRevision = SVNRevision.create(1);
		
		final SvnDiffSummarize op = svnOperationFactory.createDiffSummarize();
		op.setSource(svnRepoRoot, oldRevision, latestRevision);
		
		final Collection<SvnDiffStatus> result = new ArrayList<>(SvnDiffStatus.class);
		op.run(result);
	}
	
	/**
	 * Add a test file to working copy and commit it.
	 */
	public void testCommitFile() throws IOException, SVNException {
		// add file
		String newFileName = "TestFile_" + DateTime.now().toString().replace(':', '-');
		Path newFile = workingCopyPath.resolve(newFileName);
		Files.createFile(newFile);
		
		final SvnScheduleForAddition add = svnOperationFactory.createScheduleForAddition();
		add.setSingleTarget(SvnTarget.fromFile(newFile.toFile()));
		add.setAddParents(true);
		add.run();
		
	    final SvnCommit commit = svnOperationFactory.createCommit();
	    commit.setSingleTarget(workingCopy);// only changes below this path will be commited
	    commit.setDepth(SVNDepth.INFINITY);// TODO needed or default?
	    commit.setCommitMessage("Added file: " + newFileName);
	    commit.run();
	}
}