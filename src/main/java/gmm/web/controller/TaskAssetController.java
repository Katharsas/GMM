package gmm.web.controller;

import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.google.common.jimfs.VPath;
import com.google.common.jimfs.VPath.VPaths;

import gmm.domain.UniqueObject;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetKey;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.FileType;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.TextureTask;
import gmm.service.FileService;
import gmm.service.assets.AssetService;
import gmm.service.assets.NewAssetFileService;
import gmm.service.assets.NewAssetFolderInfo;
import gmm.service.assets.NewAssetFolderInfo.AssetFolderStatus;
import gmm.service.assets.NewAssetFolderVfs;
import gmm.service.assets.NewAssetLockService;
import gmm.service.assets.OriginalAssetFileInfo;
import gmm.service.data.DataAccess;
import gmm.service.data.PathConfig;
import gmm.service.tasks.AssetTaskService;
import gmm.service.tasks.ModelTaskService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.service.tasks.TextureTaskService;
import gmm.service.users.CurrentUser;
import gmm.web.FileTreeScript;


@Controller
@RequestMapping("tasks")
@PreAuthorize("hasRole('ROLE_USER')")
@ResponseBody
public class TaskAssetController {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired private CurrentUser user;
	@Autowired private DataAccess data;
	@Autowired private TextureTaskService textureService;
	@Autowired private ModelTaskService modelService;
	@Autowired private PathConfig config;
	@Autowired private FileService fileService;
	@Autowired private AssetService assetService;
	@Autowired private NewAssetFileService newAssetService;
	@Autowired private NewAssetLockService lockService;
	@Autowired private NewAssetFolderVfs newAssetFolderVfs;
	@Autowired private TaskServiceFinder serviceFinder;
	
	private final DateTimeFormatter expiresFormatter = 
			DateTimeFormatter.ofPattern("EEE, dd MMM yyy hh:mm:ss z").withLocale(Locale.US);
	
	/**
	 * Sets caching settings for AssetTask preview images.
	 * The image links will change whenever the images change, but they are only unique for 1 year
	 * because they append the date without year.
	 * @see {@link AssetTask#getNewestAssetCacheKey()}
	 */
	private void setPreviewCaching(HttpServletResponse response) {
		response.setHeader("Cache-Control", "Public");
		response.setHeader("Max-Age", "2419200");//cache 4 weeks
		response.setHeader("Expires", expiresFormatter.format(ZonedDateTime.now(ZoneOffset.UTC).plusYears(2)));
		response.setHeader("Pragma", "");
		
		// disable caching for testing purposes:
//		response.setHeader("Cache-Control", "no-cache");
//		response.setHeader("Pragma", "no-cache");
//		response.setHeader("Expires", "0");
	}
	
	/**
	 * Texture Preview Image
	 * -----------------------------------------------------------------
	 * @param small - true for small preview, false for full size
	 * @param version - "original" for original texture preview, newest for the most current new texture
	 * @param idLink - identifies the corresponding task
	 */
	@RequestMapping(value="/preview/texture", method = RequestMethod.GET, produces="image/png")
	public void sendTexturePreview(
			HttpServletResponse response,
			@RequestParam(value="small", defaultValue="true") boolean small,
			@RequestParam(value="ver") String version,
			@RequestParam(value="id") String idLink) throws Exception {
		
		setPreviewCaching(response);
		final TextureTask task = UniqueObject.getFromIdLink(data.getList(TextureTask.class), idLink);
		validatePreviewParams(task, version);
		textureService.writePreview(task, small, version, response.getOutputStream());
	}
	
	/**
	 * Texture Preview Image
	 * -----------------------------------------------------------------
	 * @param small - true for small preview, false for full size
	 * @param version - "original" for original texture preview, newest for the most current new texture
	 * @param idLink - identifies the corresponding task
	 */
	@RequestMapping(value="/preview/3Dmodel", method = RequestMethod.GET, produces="application/json")
	public void sendModelPreview(
			HttpServletResponse response,
			@RequestParam(value="ver") String version,
			@RequestParam(value="id") String idLink) throws Exception {
		
		setPreviewCaching(response);
		final ModelTask task = UniqueObject.getFromIdLink(data.getList(ModelTask.class), idLink);
		validatePreviewParams(task, version);
		modelService.writePreview(task, version, response.getOutputStream());
	}
	
	public void validatePreviewParams(AssetTask<?> task, String previewFilename) {
		if(task == null) throw new IllegalArgumentException("Invalid task id!");
		try {
			AssetGroupType.get(previewFilename);
		} catch(final IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid version!", e);
		}
	}
	
	/**
	 * Files
	 * -----------------------------------------------------------------
	 * This method is the serverside counterpart to the JQuery plugin used to display all files of
	 * a task as a file tree (jqueryFileTree.js). The server side logic mostly resides in the class
	 * {@link gmm.web.FileTreeScript}, which originally was a jsp scriptlet converted to a java class.
	 * 
	 * This method is called whenever the user expands a folder in the tree (recursion).
	 * 
	 * @param idLink - identifies the corresponding task
	 * @param subDir - "assets" if the files are assets
	 * @param dir - relative path to the requested directory/file
	 */
	@RequestMapping(value = {"/files/{isAssets}/{idLink}"} , method = RequestMethod.POST)
	public String[] showAssetFiles(
			@PathVariable String idLink,
			@PathVariable Boolean isAssets,
			@RequestParam("dir") Path dir) {

		final AssetTask<?> task = UniqueObject.getFromIdLink(data.getList(AssetTask.class), idLink);
		
		final NewAssetFolderInfo info = assetService.getValidNewAssetFolderInfo(task.getAssetName().getKey());
		final Path visible = config.assetsNew()
				.resolve(info.getAssetFolder())
				.resolve(isAssets ? config.subAssets() : config.subOther());
		final Path dirRelative = fileService.restrictAccess(dir, visible);
		return new FileTreeScript().html(dirRelative, visible);
	}
	
	/**
	 * Asset folder creation
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = {"/newAssetFolder/{idLink}"} , method = RequestMethod.POST)
	public String[] showNewAssetFolderTree(
			@PathVariable String idLink,
			@RequestParam("dir") String dir) {
		
		final AssetTask<?> task = UniqueObject.getFromIdLink(data.getList(AssetTask.class), idLink);
		final AssetTaskService<?> service = serviceFinder.getAssetService(task.getAssetName().getKey());
		
		final VPaths vPaths = newAssetFolderVfs.virtualPaths();
		final VPath subFolder = vPaths.of(service.getAssetTypeSubFolder());
		return new FileTreeScript().html(vPaths.of(dir), vPaths.root, Optional.of(subFolder));
	}
	
	/**
	 * @param path - relative to new asset folder
	 */
	@RequestMapping(value = "/createAssetFolder/{idLink}", method = RequestMethod.POST)
	public ResponseEntity<Void> createAssetFolder(
			@PathVariable final String idLink,
			@RequestParam("path") final Path path) {
		
		final AssetTask<?> task = UniqueObject.getFromIdLink(data.getList(AssetTask.class), idLink);
		final AssetKey assetKey = task.getAssetName().getKey();
		final AssetTaskService<?> service = serviceFinder.getAssetService(assetKey);
		final Path insideTypeFolder = fileService.restrictAccess(path, service.getAssetTypeSubFolder());
		final Path assetFolderPath = service.getAssetTypeSubFolder().resolve(insideTypeFolder).resolve(task.getAssetName().get());
		
		return tryAquireNewAssetLock(() -> {
			if (!newAssetFolderVfs.isValidAssetFolderLocation(assetFolderPath)) {
				throw new IllegalArgumentException("Path '" + path + "' is not a valid new asset folder location!");
			}
			newAssetService.createNewAssetFolder(assetKey, assetFolderPath, user.get());
			return new ResponseEntity<>(HttpStatus.OK);
		});
	}
	
	/**
	 * @param idLink
	 * @param assetName - without extension i guess
	 */
	@RequestMapping(value = "/renameAsset/{idLink}/{assetName}")
	public void renameAsset(
			@PathVariable final String idLink,
			@PathVariable final String assetName) {
		// TODO validate that idLink is an asset task
		// find asset folder if exist, rename it, rename asset if exits (make sure not other operation is running)
		// commit, rescan to to relink, edit event
	}
	
	// TODO FileType "WIP" should not be hardcoded string?
	/**
	 * Upload a wip file.
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 */
	@RequestMapping(value = {"/upload/WIP/{idLink}"} , method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Void> handleWipFileUpload(
			HttpServletRequest request,
			@PathVariable String idLink) {
		
		final MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		final MultiValueMap<String, MultipartFile> map = multipartRequest.getMultiFileMap();
		final MultipartFile file = map.getFirst("file");
		if (file.isEmpty()) {
			throw new IllegalArgumentException("Uploaded file is empty. Upload not successful!");
		}
		final AssetKey assetKey = AssetKey.getFromIdLink(data.getList(AssetTask.class), idLink);
		return tryAquireNewAssetLock(() -> {
			newAssetService.addWipFile(assetKey, file, user.get());
			return new ResponseEntity<>(HttpStatus.OK);
		});
	}
	
	// TODO FileType "ASSET" should not be hardcoded string?
	/**
	 * Upload an asset.
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 */
	@RequestMapping(value = {"/upload/ASSET/{idLink}"} , method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Void> handleAssetUpload(
			HttpServletRequest request,
			@PathVariable String idLink) {
		
		final MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		final MultiValueMap<String, MultipartFile> map = multipartRequest.getMultiFileMap();
		final MultipartFile file = map.getFirst("file");
		if (file.isEmpty()) {
			throw new IllegalArgumentException("Uploaded file is empty. Upload not successful!");
		}
		final AssetKey assetKey = AssetKey.getFromIdLink(data.getList(AssetTask.class), idLink);
		return tryAquireNewAssetLock(() -> {
			newAssetService.addAssetFile(assetKey, file, user.get());
			return new ResponseEntity<>(HttpStatus.OK);
		});
	}
	
	/**
	 * Download an asset.
	 */
	@RequestMapping(value = {"/download/{idLink}/{groupType}/ASSET/"},
			method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<Resource> handleAssetDownload(final HttpServletResponse response,
			@PathVariable final String idLink,
			@PathVariable final AssetGroupType groupType) {
		
		final AssetKey assetKey = AssetKey.getFromIdLink(data.getList(AssetTask.class), idLink);
		final Path absolute;
		if (groupType.isOriginal()) {
			final OriginalAssetFileInfo info = assetService.getOriginalAssetFileInfo(assetKey);
			Assert.notNull(info, "");
			absolute = info.getAssetFilePathAbsolute(config);
		} else {
			final NewAssetFolderInfo info = assetService.getNewAssetFolderInfo(assetKey);
			Assert.notNull(info, "");
			Assert.isTrue(info.getStatus() == AssetFolderStatus.VALID_WITH_ASSET, "");
			absolute = info.getAssetFilePathAbsolute(config);
		}
		
		final Supplier<ResponseEntity<Resource>> getReturnFile = () -> {
			final HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Disposition", "attachment; filename=\"" + absolute.getFileName() + "\"");
			final Resource file = new FileSystemResource(absolute.toFile());
			return new ResponseEntity<>(file, headers, HttpStatus.OK);
		};
		if (groupType.isOriginal()) {
			return getReturnFile.get();
		} else {
			return tryAquireNewAssetLock(getReturnFile);
		}
	}
	
	/**
	 * Download any file associated with given asset (WIP files).
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 * @param fileType - one of the enum values of its type {@link FileType}
	 * @param relativeFile - path to the downloaded file relative to asset folder and  {@link FileType} subfolder
	 */
	@RequestMapping(value = {"/download/{idLink}/NEW/{fileType}/{relativeFile}/"},
			method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<Resource> handleOtherDownload(
			@PathVariable final String idLink,
			@PathVariable final FileType fileType,
			@PathVariable final Path relativeFile) {
		
		final AssetKey assetKey = AssetKey.getFromIdLink(data.getList(AssetTask.class), idLink);
		final NewAssetFolderInfo info = assetService.getValidNewAssetFolderInfo(assetKey);
		final Path assetFolder = config.assetsNew().resolve(info.getAssetFolder());
		final Path visible = assetFolder.resolve(fileType.getSubPath(config));
		final Path absolute = visible.resolve(fileService.restrictAccess(relativeFile, visible));
		
		return tryAquireNewAssetLock(() -> {
			final HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Disposition", "attachment; filename=\"" + absolute.getFileName() + "\"");
			final Resource file = new FileSystemResource(absolute.toFile());
			return new ResponseEntity<>(file, headers, HttpStatus.OK);
		});
	}
	
	/**
	 * Delete File
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 * @param isAsset - true if file is an asset
	 * @param relativeFile - relative path to the deleted file (only if not asset)
	 * @return 
	 */
	@RequestMapping(value = {"/deleteFile/{idLink}"} , method = RequestMethod.POST)
	public ResponseEntity<Void> handleDeleteFile(
			@PathVariable String idLink,
			@RequestParam("asset") Boolean isAsset,
			@RequestParam(value="dir", required=false) Path relativeFile) {
		
		final AssetKey assetKey = AssetKey.getFromIdLink(data.getList(AssetTask.class), idLink);
		if (!isAsset && relativeFile == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return tryAquireNewAssetLock(() -> {
			if (isAsset) {
				newAssetService.deleteAssetFile(assetKey, user.get());
			} else {
				newAssetService.deleteWipFile(assetKey, relativeFile, user.get());
			}
			return new ResponseEntity<>(HttpStatus.OK);
		});
	}
	
	private <T> ResponseEntity<T> tryAquireNewAssetLock(Supplier<ResponseEntity<T>> runIfAquired) {
		if(lockService.tryLock("TaskAssetController::tryAquireNewAssetLock")) {
			logger.debug("Successfully aquired new asset lock.");
			try {
				return runIfAquired.get();
			} finally {
				lockService.unlock("TaskAssetController::tryAquireNewAssetLock");
			}
		} else {
			logger.info("Failed to aquire new asset lock!");
			return new ResponseEntity<>(HttpStatus.LOCKED);
		}
	}
	
	@RequestMapping(value = {"/newAssetFileOperationsEnabled"} , method = RequestMethod.GET)
	public boolean isNewAssetFileOperationsEnabled() {
		return lockService.isAvailable();
	}
}
