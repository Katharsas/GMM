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
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import gmm.collections.HashSet;
import gmm.domain.task.asset.AssetTypeService;
import gmm.service.tasks.AssetTaskService;

@Service
public class VCSService {
	
	public static class SVNTest {
		
		// TODO Open Working Copy (= client) or checkout one
		// TODO hook into repository to get changes always, immediately
		// TODO when access to repository is not possible, any file uploads cannot be made and must be disabled.
		// TODO when access to repository is not possible, file downloads must be disabled because the files could
		// be outdated and the GMM woudn't know that.
		
		public static void initAndCheckoutWithOldApi() throws SVNException {
			
			final SVNURL svnRoot = SVNURL.fromFile(new File("C:/SVNServer/trunk/project/newAssets"));
			
			final ISVNAuthenticationManager authManager =
	                   SVNWCUtil.createDefaultAuthenticationManager("(login name)", "(login password)".toCharArray());
			
//			// Once (initialize File System driver):
//			FSRepositoryFactory.setup();
//			
//			// Open session (needed because could be changed from FS driver to https driver):
//			SVNRepository repository = SVNRepositoryFactory.create(svnRoot);
//			
//			repository.setAuthenticationManager(authManager);
//			repository.closeSession();
			
			final SVNClientManager clientManager = SVNClientManager.newInstance(null, authManager);
			
			final SVNUpdateClient updateClient = clientManager.getUpdateClient( );
			
			final File workingCopyPath = new File("workspace/newAssets");
			updateClient.doCheckout(svnRoot, workingCopyPath, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
		}
		
		
		public static void initAndCheckoutWithNewApi() throws SVNException {
			
			final SvnTarget svnRoot = SvnTarget.fromFile(new File("C:/SVNServer/trunk/project/newAssets"));
			final SvnTarget workingCopyPath = SvnTarget.fromFile(new File("workspace/newAssets"));
			
			final ISVNAuthenticationManager authManager =
	                   SVNWCUtil.createDefaultAuthenticationManager("(login name)", "(login password)".toCharArray());
			
			final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
			svnOperationFactory.setAuthenticationManager(authManager);
			try {
			    final SvnCheckout checkout = svnOperationFactory.createCheckout();
			    checkout.setSingleTarget(workingCopyPath);
			    checkout.setSource(svnRoot);
			    checkout.setRevision(SVNRevision.HEAD);// TODO: needed? maybe even wrong?
			    //... other options
			    checkout.run();
			} finally {
			    svnOperationFactory.dispose();
			}
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
			// The update can cause very costly operations like generating previews for all changed files.
			// Question:
			// Do we just re-initialize all files on every update?
			// -> This means new previews get generated and so on.
			// Or do we somehow keep track of which files changed and which didnt?
			// We mostly need to determine then:
			// - Do all mapped folders and assetfiles still exist? 
			// - Did the status of an asset folder change (become invalid or valid etc.) ?
			// - Did the asset file itself change (either by hash or by somehow keeping track of svn changes) ?
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
