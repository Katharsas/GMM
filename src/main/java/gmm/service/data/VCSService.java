package gmm.service.data;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnDiffStatus;
import org.tmatesoft.svn.core.wc2.SvnDiffSummarize;
import org.tmatesoft.svn.core.wc2.SvnGetInfo;
import org.tmatesoft.svn.core.wc2.SvnInfo;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpdate;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.HashSet;
import gmm.domain.task.asset.AssetTypeService;
import gmm.service.tasks.AssetTaskService;

@Service
public class VCSService {
	
	public static class SVNTest {
		
		final SvnTarget svnRepoRoot = SvnTarget.fromFile(new File("C:/SVNServer/trunk/project/newAssets"));
		
		final SvnTarget workingCopyPath = SvnTarget.fromFile(new File("workspace/newAssets"));
		
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
		    checkout.setSingleTarget(workingCopyPath);
		    checkout.setSource(svnRepoRoot);
		    
		    final long revision = checkout.run();
		    svnOperationFactory.dispose();
		}
		
		/**
		 * Update(SVN) / Fetch(Git) local working copy with latest files from repo.
		 */
		public void updateWorkingCopy() throws SVNException {
			
			final SvnUpdate update = svnOperationFactory.createUpdate();
			update.setSingleTarget(workingCopyPath);
			
			final long[] revisions = update.run();
		}
		
		/**
		 * Find out at which revision number the repo is at.
		 * Can also be used on single files to find out in which revision the last changed occured to them.
		 */
		public void latestRevision() throws SVNException {
			
			final SvnGetInfo operation = svnOperationFactory.createGetInfo();
			operation.setSingleTarget(workingCopyPath);
			
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
		
	}
	
	
	public void onVcsRepoChanged() {
		
		// TODO make sure working copy is up to date when this is called
		
		final Map<Path, AssetTaskService<?>> assetTypeFolders = getVcsAssetTypeFolders();
		
		final Map<String, AssetFolderInfo> allAssetFolders = new CaseInsensitiveMap<>();
		
		for (final Entry<Path, AssetTaskService<?>> entry : assetTypeFolders.entrySet()) {
			
			final Path assetTypeFolder = entry.getKey();
			final AssetTaskService<?> service = entry.getValue();
			
			final BiConsumer<String, AssetFolderInfo> onHit = (folderName, folderInfo) -> {
				final AssetFolderInfo duplicate = allAssetFolders.get(folderName);
				if (duplicate != null) {
					allAssetFolders.put(folderName, AssetFolderInfo.createInvalidNotUnique(duplicate, folderInfo));
				} else {
					allAssetFolders.put(folderName, folderInfo);
				}
			};
			
			scanVcsAssetTypeFolder(assetTypeFolder, service, onHit);
			
			// TODO
			// There must be mapping between AssetTasks and AssetFolder.
			// This mapping must be updated to reflect changes since the last update.
			// Thus, when we know which files changed from SVN diff, we need to determine:
			// - Do all mapped folders and assetfiles still exist? 
			// - Did the status of an asset folder change (become invalid or valid etc.) ?
			// - Did the asset file itself change (save revision the gmm is currently at, get changed files since that revision from svn)
			// - What about multiple asset folders, were all duplicates removed, or some added ?
			// Then only the needed actions could ne taken.
			
		}
	}
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public DataConfigService config;
	
	@Autowired
	private AssetTypeService assetTypeService;

	public static enum AssetFolderStatus {
		
		INVALID_ASSET_FOLDER_NOT_UNIQUE(false),
		INVALID_ASSET_FOLDER_EXTENSION(false),
		INVALID_ASSET_FOLDER_CONTENT(false),
		INVALID_ASSET_FILE_NAME(false),
		
		VALID_WITH_ASSET(true),
		VALID_NO_ASSET(true);
		
		public final boolean isValid;
		AssetFolderStatus(boolean isValid) {
			this.isValid = isValid;
		}
	}
	
	public static class AssetFolderInfo {
		
		public static AssetFolderInfo createInvalidNotUnique(AssetFolderInfo duplicate, AssetFolderInfo current) {
			return new AssetFolderInfo(duplicate, current);
		}
		
		private final Path assetFolder;
		private final String assetFileName;
		
		private final AssetFolderStatus status;
		
		private final HashSet<Path> nonUniqueDuplicates;
		
		private AssetFolderInfo(AssetFolderInfo duplicate, AssetFolderInfo current) {
			this.assetFolder = null;
			this.assetFileName = null;
			
			final AssetFolderStatus notUnique = AssetFolderStatus.INVALID_ASSET_FOLDER_NOT_UNIQUE;
			this.status = notUnique;
			this.nonUniqueDuplicates = new HashSet<>(Path.class);
			
			if (duplicate.status == notUnique) {
				this.nonUniqueDuplicates.addAll(duplicate.nonUniqueDuplicates);
			} else {
				this.nonUniqueDuplicates.add(duplicate.assetFolder);
			}
			
			if (current.status == notUnique) {
				this.nonUniqueDuplicates.addAll(current.nonUniqueDuplicates);
			} else {
				this.nonUniqueDuplicates.add(current.assetFolder);
			}
		}
		
		/**
		 * @param base - absolute path up to and including the asset type folder inside svn root, must exist
		 * @param relative - relative to assetTypeFolder, pointing to an asset folder inside base, must exist
		 */
		public AssetFolderInfo(AssetTaskService<?> service, Path base, Path relative) {
			this.nonUniqueDuplicates = null;
			this.assetFolder = relative;
			
			final Path assetFolderAbs = base.resolve(relative);
			
			final String assetFolderName = assetFolderAbs.getFileName().toString();
			final boolean isValidAssetFolderName = service.getExtensions().test(assetFolderName);
			
			String assetFileName = null;
			
			if (!isValidAssetFolderName) {
				status = AssetFolderStatus.INVALID_ASSET_FOLDER_EXTENSION;
			} else {
				final List<Path> files;
				try {
					files = Files.list(assetFolderAbs)
						.filter(path -> path.toFile().isFile())
						.collect(Collectors.toList());
					
				} catch (final IOException e) {
					throw new UncheckedIOException(e);
				}
				
				if (files.size() > 1) {
					status = AssetFolderStatus.INVALID_ASSET_FOLDER_CONTENT;
				} else if (files.size() == 0) {
					status = AssetFolderStatus.VALID_NO_ASSET;
				} else {
					final Path assetFile = files.iterator().next();
					assetFileName = assetFile.getFileName().toString();
					
					if (!assetFolderName.equalsIgnoreCase(assetFileName)) {
						status = AssetFolderStatus.INVALID_ASSET_FILE_NAME;
					} else  {
						status = AssetFolderStatus.VALID_WITH_ASSET;
					}
				}
			}
			this.assetFileName = assetFileName;
		}
	}
	
	private Map<Path, AssetTaskService<?>> getVcsAssetTypeFolders() {
		
		final Map<Path, AssetTaskService<?>> result = new HashMap<>();
		
		for (final AssetTaskService<?> service : assetTypeService.getAssetTaskServices()) {
			
			final Path assetTypeFolder = config.assetsNew().resolve(service.getAssetTypeSubFolder());
			final String taskTypeName = service.getTaskType().getSimpleName();
			
			if (!assetTypeFolder.toFile().isDirectory()) {
				logger.warn("Could not find asset folder for type '" + taskTypeName + "' at '" + assetTypeFolder + "'!");
			} else {
				logger.info("Found asset type folder for type '" + taskTypeName + "' at '" + assetTypeFolder + "'.");
				
				result.put(assetTypeFolder, service);
			}
		}
		return result;
	}
	
	public void scanVcsAssetTypeFolder(Path assetTypeFolder, AssetTaskService<?> service,
			BiConsumer<String, AssetFolderInfo> onHit) {

		if (!assetTypeFolder.toFile().isDirectory()) {
			throw new IllegalArgumentException("Directory expected!");
		}
		
		try {
			Files.walkFileTree(assetTypeFolder, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					
					final String dirName = dir.getFileName().toString();
					if (isAssetFolderByConvention(dirName)) {
						
						final Path relative = assetTypeFolder.relativize(dir);
						final AssetFolderInfo folderInfo = new AssetFolderInfo(service, assetTypeFolder, relative);
						onHit.accept(dirName, folderInfo);
						
						return FileVisitResult.SKIP_SUBTREE;
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private boolean isAssetFolderByConvention(String folderName) {
		// by convention, an asset folder is named like a file, with file name and file extension
		// separated by a point.
		final String[] folderNameParts = folderName.split(".");
		return folderNameParts.length >= 2;
	}
	
}
