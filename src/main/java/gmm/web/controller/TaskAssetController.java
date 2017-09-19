package gmm.web.controller;

import java.nio.file.Path;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.domain.UniqueObject;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.FileType;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.TextureTask;
import gmm.service.FileService;
import gmm.service.assets.AssetService;
import gmm.service.assets.NewAssetFolderInfo;
import gmm.service.assets.NewAssetFolderInfo.AssetFolderStatus;
import gmm.service.assets.OriginalAssetFileInfo;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.tasks.ModelTaskService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.service.tasks.TextureTaskService;
import gmm.web.FileTreeScript;
import gmm.web.sessions.tasklist.WorkbenchSession;


@Controller
@RequestMapping("tasks")
@PreAuthorize("hasRole('ROLE_USER')")

public class TaskAssetController {

	@Autowired WorkbenchSession session;
	@Autowired DataAccess data;
	@Autowired TaskServiceFinder taskService;
	@Autowired TextureTaskService textureService;
	@Autowired ModelTaskService modelService;
	@Autowired DataConfigService config;
	@Autowired FileService fileService;
	@Autowired AssetService assetService;
	
	private final DateTimeFormatter expiresFormatter = 
			DateTimeFormat.forPattern("EEE, dd MMM yyy hh:mm:ss z").withLocale(Locale.US);
	
	/**
	 * Sets caching settings for AssetTask preview images.
	 * The image links will change whenever the images change, but they are only unique for 1 year
	 * because they append the date without year.
	 * @see {@link AssetTask#getNewestAssetCacheKey()}
	 */
	private void setPreviewCaching(HttpServletResponse response) {
		response.setHeader("Cache-Control", "Public");
		response.setHeader("Max-Age", "2419200");//cache 4 weeks
		response.setHeader("Expires", DateTime.now().plusYears(2).toString(expiresFormatter));
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
	public @ResponseBody void sendTexturePreview(
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
	public @ResponseBody void sendModelPreview(
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
	public @ResponseBody String[] showAssetFiles(
			@PathVariable String idLink,
			@PathVariable Boolean isAssets,
			@RequestParam("dir") Path dir) {

		final AssetTask<?> task = UniqueObject.getFromIdLink(data.getList(AssetTask.class), idLink);
		
		final NewAssetFolderInfo info = assetService.getNewAssetFolderInfo(task.getAssetName());
		if (info == null) return new String[] {};
		else {
			if (!info.getStatus().isValid) return new String[] {};
			else {
				final Path visible = config.assetsNew()
						.resolve(info.getAssetFolder())
						.resolve(isAssets ? config.subAssets() : config.subOther());
				final Path dirRelative = fileService.restrictAccess(dir, visible);
				return new FileTreeScript().html(dirRelative, visible);
			}
		}
	}
	
//	/**
//	 * Upload File
//	 * -----------------------------------------------------------------
//	 * @param idLink - identifies the corresponding task
//	 */
//	@RequestMapping(value = {"/upload/{idLink}"} , method = RequestMethod.POST)
//	@ResponseBody
//	public void handleUpload(
//			HttpServletRequest request,
//			@PathVariable String idLink) throws Exception {
//		
//		final MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
//		final MultiValueMap<String, MultipartFile> map = multipartRequest.getMultiFileMap();
//		final MultipartFile file = map.getFirst("file");
//		if (file.isEmpty()) throw new IllegalArgumentException("Uploaded file is empty. Upload not successful!");
//		
//		final AssetTask<?> task = UniqueObject.getFromIdLink(data.getList(AssetTask.class), idLink);
//		taskService.addFile(task, file);
//	}
	
	@RequestMapping(value = {"/download/{idLink}/{groupType}/ASSET/"},
			method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public FileSystemResource handleAssetDownload(final HttpServletResponse response,
			@PathVariable final String idLink,
			@PathVariable final AssetGroupType groupType) {
		
		final AssetTask<?> task = UniqueObject.getFromIdLink(data.getList(AssetTask.class), idLink);
		final AssetName name = task.getAssetName();
		final Path absolute;
		if (groupType.isOriginal()) {
			final OriginalAssetFileInfo info = assetService.getOriginalAssetFileInfo(name);
			Assert.notNull(info);
			absolute = info.getAssetFilePathAbsolute(config);
		} else {
			final NewAssetFolderInfo info = assetService.getNewAssetFolderInfo(name);
			Assert.notNull(info);
			Assert.isTrue(info.getStatus() == AssetFolderStatus.VALID_WITH_ASSET);
			absolute = info.getAssetFilePathAbsolute(config);
		}
		response.setHeader("Content-Disposition", "attachment; filename=\""+absolute.getFileName()+"\"");
		return new FileSystemResource(absolute.toFile());
	}
	
	/**
	 * Download File
	 * 
	 * Downloads file from an asset task shown in view. Can be used to download the files
	 * shown in the preview or to download files shown in the task file manager.
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 * @param fileType - on of the enum values of its type {@link FileType}
	 * @param dir - relative path to the downloaded file (if preview: "original" or "newest")
	 */
	@RequestMapping(value = {"/download/{idLink}/NEW/{fileType}/{relativeFile}/"},
			method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public FileSystemResource handleOtherDownload(final HttpServletResponse response,
			@PathVariable final String idLink,
			@PathVariable final FileType fileType,
			@PathVariable final Path relativeFile) {
		
		final AssetTask<?> task = UniqueObject.getFromIdLink(data.getList(AssetTask.class), idLink);
		final NewAssetFolderInfo info = assetService.getNewAssetFolderInfo(task.getAssetName());
		Assert.notNull(info);
		Assert.isTrue(info.getStatus().isValid);
		final Path assetFolder = config.assetsNew().resolve(info.getAssetFolder());
		final Path visible = assetFolder.resolve(fileType.getSubPath(config));
		final Path absolute = visible.resolve(fileService.restrictAccess(relativeFile, visible));
		
		response.setHeader("Content-Disposition", "attachment; filename=\""+absolute.getFileName()+"\"");
		return new FileSystemResource(absolute.toFile());
	}
	
	/**
	 * Delete File
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 * @param isAsset - true if file is an asset
	 * @param relativeFile - relative path to the deleted file (only if not asset)
	 */
	@RequestMapping(value = {"/deleteFile/{idLink}"} , method = RequestMethod.POST)
	@ResponseBody
	public void handleDeleteFile(
			@PathVariable String idLink,
			@RequestParam("asset") Boolean isAsset,
			@RequestParam(value="dir", required=false) Path relativeFile) {
		
		final AssetTask<?> task = UniqueObject.getFromIdLink(data.getList(AssetTask.class), idLink);
		final FileType fileType = isAsset ? FileType.ASSET : FileType.WIP;
		if (isAsset) {
			assetService.deleteAssetFile(task.getAssetName());
		} else {
			Assert.notNull(relativeFile);
			assetService.deleteFile(task.getAssetName(), fileType, relativeFile);
		}
	}
}
