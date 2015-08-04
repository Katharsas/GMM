package gmm.web.controller;

import java.nio.file.Path;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import gmm.domain.UniqueObject;
import gmm.domain.task.AssetTask;
import gmm.domain.task.TextureTask;
import gmm.service.FileService;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.service.tasks.TextureTaskService;
import gmm.web.FileTreeScript;
import gmm.web.sessions.TaskSession;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@Controller
@RequestMapping("tasks")
@PreAuthorize("hasRole('ROLE_USER')")

public class TaskAssetController {

	@Autowired TaskSession session;
	@Autowired DataAccess data;
	@Autowired TaskServiceFinder taskService;
	@Autowired TextureTaskService textureService;
	@Autowired DataConfigService config;
	@Autowired FileService fileService;
	
	private final DateTimeFormatter expiresFormatter = 
			DateTimeFormat.forPattern("EEE, dd MMM yyy hh:mm:ss z").withLocale(Locale.US);
	
	/**
	 * Sets caching settings for AssetTask preview images.
	 * The image links will change whenever the images change, but they are only
	 * unique for 4 weeks because they append the date without year/month.
	 * @see {@link AssetTask#getNewestAssetNocache()}
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
	@RequestMapping(value="/preview", method = RequestMethod.GET, produces="image/png")
	public @ResponseBody byte[] sendPreview(
			HttpServletResponse response,
			@RequestParam(value="small", defaultValue="false") boolean small,
			@RequestParam(value="ver") String version,
			@RequestParam(value="id") String idLink) throws Exception {
		
		setPreviewCaching(response);
		TextureTask task = UniqueObject.<TextureTask>getFromIdLink(data.<TextureTask>getList(TextureTask.class), idLink);
		return textureService.getPreview(task, small, version);
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
	@RequestMapping(value = {"/files/{subDir}/{idLink}"} , method = RequestMethod.POST)
	public @ResponseBody String[] showAssetFiles(
			@PathVariable String idLink,
			@PathVariable String subDir,
			@RequestParam("dir") Path dir) {

		AssetTask<?> task = (AssetTask<?>) UniqueObject.getFromIdLink(data.getList(AssetTask.class), idLink);
		boolean isAssets = subDir.equals("assets");
		
		Path visible = config.ASSETS_NEW
				.resolve(task.getAssetPath())
				.resolve(isAssets ? config.SUB_ASSETS : config.SUB_OTHER);
		dir = fileService.restrictAccess(dir, visible);
		return new FileTreeScript().html(dir, visible);
	}
	
	/**
	 * Upload File
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 */
	@RequestMapping(value = {"/upload/{idLink}"} , method = RequestMethod.POST)
	@ResponseBody
	public void handleUpload(
			HttpServletRequest request,
			@PathVariable String idLink) throws Exception {
		
		MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		MultiValueMap<String, MultipartFile> map = multipartRequest.getMultiFileMap();
		MultipartFile file = (MultipartFile) map.getFirst("file");
		if (file.isEmpty()) throw new IllegalArgumentException("Uploaded file is empty. Upload not successful!");
		
		AssetTask<?> task = (AssetTask<?>) UniqueObject.getFromIdLink(data.getList(AssetTask.class), idLink);
		taskService.addFile(task, file);
	}
	
	/**
	 * Download File
	 * 
	 * Downloads file from an asset task shown in view. Can be used to download the files
	 * shown in the preview or to download files shown in the task file manager.
	 * To download from preview, subDir argument must equal "preview".
	 * To download from manager, subDir argument must euqla "asset" or "other",
	 * depending on to which of the two file managers of this task the file belongs.
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 * @param subDir - "preview", "asset" or "other"
	 * @param dir - relative path to the downloaded file (if preview: "original" or "newest")
	 */
	@RequestMapping(value = {"/download/{idLink}/{subDir}/{dir}/"},
			method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public FileSystemResource handleDownload(final HttpServletResponse response,
			@PathVariable final String idLink,
			@PathVariable final String subDir,
			@PathVariable final String dir) {
		AssetTask<?> task = (AssetTask<?>) UniqueObject.getFromIdLink(data.getList(AssetTask.class), idLink);
		Path filePath;
		Path base;
		if(subDir.equals("preview")) {
			if(dir.equals("original")) {
				base = config.ASSETS_ORIGINAL;
				filePath = task.getOriginalAssetPath();
			}
			else if(dir.equals("newest")) {
				base = config.ASSETS_NEW;
				filePath = task.getNewestAssetPath();
			}
			else throw new IllegalArgumentException("Preview file version '"+dir+"' is invalid. Valid values are 'original' and 'newest'");
		}
		else if (subDir.equals("asset") || subDir.equals("other") ){
			boolean isAssets = subDir.equals("asset");
			base = config.ASSETS_NEW;
			filePath = task.getAssetPath()
					.resolve(isAssets ? config.SUB_ASSETS : config.SUB_OTHER)
					.resolve(dir);
		}
		else throw new IllegalArgumentException("Sub directory '"+subDir+"' is invalid. Valid values are 'preview', 'asset' and 'other'");
		filePath = base.resolve(fileService.restrictAccess(filePath, base));
		response.setHeader("Content-Disposition", "attachment; filename=\""+filePath.getFileName()+"\"");
		return new FileSystemResource(filePath.toFile());
	}
	
	/**
	 * Delete File
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 * @param asset - true if file is an asset
	 * @param dir - relative path to the deleted file
	 */
	@RequestMapping(value = {"/deleteFile/{idLink}"} , method = RequestMethod.POST)
	@ResponseBody
	public void handleDeleteFile(
			@PathVariable String idLink,
			@RequestParam("asset") Boolean asset,
			@RequestParam("dir") Path dir) throws Exception {
		
		AssetTask<?> task = (AssetTask<?>) UniqueObject.getFromIdLink(data.getList(AssetTask.class), idLink);
		taskService.deleteFile(task, dir, asset);
	}

}
