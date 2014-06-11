package gmm.web.controller;

import java.nio.file.Path;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import gmm.domain.AssetTask;
import gmm.domain.TextureTask;
import gmm.domain.UniqueObject;
import gmm.service.FileService;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.tasks.TextureService;
import gmm.web.AjaxResponseException;
import gmm.web.FileTreeScript;
import gmm.web.sessions.TaskSession;

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
	@Autowired TextureService assetService;
	@Autowired DataConfigService config;
	@Autowired FileService fileService;
	
	private void setHeaderCaching(HttpServletResponse response) {
//		Calendar date = new GregorianCalendar(3000, 1, 1);
//		SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss z", Locale.US);
//		
		response.setHeader("Cache-Control", "Public");
//		response.setHeader("Max-Age", "2629000");
//		response.setHeader("Pragma", "");
//		response.setHeader("Expires", formatter.format(date.getTime()));
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
			@RequestParam(value="id") String idLink) throws AjaxResponseException {
		try {
			setHeaderCaching(response);
			//TODO enable reasonable caching
			TextureTask task = UniqueObject.<TextureTask>getFromId(data.<TextureTask>getList(TextureTask.class), idLink);
			return assetService.getPreview(task, small, version);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
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
	 * @throws AjaxResponseException 
	 */
	@RequestMapping(value = {"/files/{subDir}/{idLink}"} , method = RequestMethod.POST)
	public @ResponseBody String showAssetFiles(
			@PathVariable String idLink,
			@PathVariable String subDir,
			@RequestParam("dir") Path dir) throws AjaxResponseException {
		try {
			AssetTask task = (AssetTask) UniqueObject.getFromId(session.getTasks(), idLink);
			boolean isAssets = subDir.equals("assets");
			
			Path visible = config.ASSETS_NEW
					.resolve(task.getNewAssetFolder())
					.resolve(isAssets ? config.SUB_ASSETS : config.SUB_OTHER);
			dir = fileService.restrictAccess(dir, visible);
			return new FileTreeScript().html(dir, visible);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Upload File
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 * @throws AjaxResponseException 
	 */
	@RequestMapping(value = {"/upload/{idLink}"} , method = RequestMethod.POST)
	@ResponseBody
	public String handleUpload(
			HttpServletRequest request,
			@PathVariable String idLink) throws AjaxResponseException {
		try {
			MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
			MultiValueMap<String, MultipartFile> map = multipartRequest.getMultiFileMap();
			MultipartFile file = (MultipartFile) map.getFirst("myFile");
			
			TextureTask task = (TextureTask) UniqueObject.getFromId(session.getTasks(), idLink);
			assetService.addTextureFile(file, task);
			
			return file.isEmpty()? "Upload failed!" : "Upload successfull!";
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Download File
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 * @param subDir - "asset" if the file is an asset
	 * @param dir - relative path to the downloaded file
	 * @throws AjaxResponseException 
	 */
	@RequestMapping(value = {"/download/{idLink}/{subDir}/{dir}/"},
			method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public FileSystemResource handleDownload(
			HttpServletResponse response,
			@PathVariable String idLink,
			@PathVariable String subDir,
			@PathVariable Path dir) throws AjaxResponseException {
		try {
			AssetTask task = (AssetTask) UniqueObject.getFromId(session.getTasks(), idLink);
			boolean isAssets = subDir.equals("assets");
			
			Path filePath = config.ASSETS_NEW
					.resolve(task.getNewAssetFolder())
					.resolve(isAssets ? config.SUB_ASSETS : config.SUB_OTHER)
					.resolve(dir);
			response.setHeader("Content-Disposition", "attachment; filename=\""+filePath.getFileName()+"\"");
			return new FileSystemResource(filePath.toFile());
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Delete File
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 * @param asset - true if file is an asset
	 * @param dir - relative path to the deleted file
	 * @throws AjaxResponseException 
	 */
	@RequestMapping(value = {"/deleteFile/{idLink}"} , method = RequestMethod.POST)
	@ResponseBody
	public void handleDeleteFile(
			@PathVariable String idLink,
			@RequestParam("asset") Boolean asset,
			@RequestParam("dir") Path dir) throws AjaxResponseException {
		try {
			TextureTask task = (TextureTask) UniqueObject.getFromId(session.getTasks(), idLink);
			assetService.deleteTextureFile(dir, asset, task);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}

}
