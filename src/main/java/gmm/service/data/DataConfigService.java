package gmm.service.data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gmm.domain.task.asset.AssetGroupType;
import gmm.service.FileService;


@Service
public class DataConfigService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final FileService fileService;
	
	private final Path base;
	
	/**
	 * Absolute or relative to base.
	 */
	@Value("${path.workspace}") private Path workspace;
	
	/*
	 * Absolute or relative to workspace:
	 */
	
	@Value("${path.assets.original}") private Path assetsOriginal;
	@Value("${path.assets.new}") private Path assetsNew;
	@Value("${path.assets.previews}") private Path assetPreviews;
	
	@Value("${path.users}") private Path dbUsers;
	@Value("${path.tasks}") private Path dbTasks;
	@Value("${path.other}") private Path dbOther;
	
//	@Value("${path.upload}") private Path upload;
	
	@Value("${path.blender}") private Path blender;
	
	/*
	 * Absolute paths, updated on workspace change:
	 */
	
	private Path WORKSPACE;
	
	private Path USERS;
	private Path ASSETS_ORIGINAL;
	private Path ASSETS_NEW;
	private Path ASSET_PREVIEWS;
	private Path TASKS;
//	private Path UPLOAD;
	private Path DB_OTHER;
	private Path BLENDER;
	
	/*
	 * Relative sub paths:
	 */
	
	@Value("${path.assets.new.tga}") private Path subNewTextures;
	@Value("${path.assets.new.3ds}") private Path subNewModels;
//	@Value("${path.assets.new.worlds}") private Path subNewWorlds;
	
	private final Path subAssets = Paths.get(".");
//	private final Path subPreview = Paths.get("preview");
	private final Path subOther = Paths.get("wip");
	
	@Autowired
	public DataConfigService(FileService fileService, ServletContext context) {
		this.fileService = fileService;
		base = Paths.get(context.getRealPath(""));
	}
	
	@PostConstruct
	private void setUpConfigService() {
		updateWorkspace(workspace);
		BLENDER = base.resolve(blender).normalize();
		logger.info("\n"
				+	"##########################################################" + "\n\n"
				+	"  Expecting blender installation folder to be at: " + "\n"
				+	"  " + blender + "\n\n"
				+	"##########################################################");
	}
	
	protected void updateWorkspace(Path workspace) {
		Objects.requireNonNull(workspace);
		WORKSPACE = base.resolve(workspace).normalize();
		
		logger.info("\n"
				+	"##########################################################" + "\n\n"
				+	"  Registered workspace folder at: " + "\n"
				+	"  " + WORKSPACE + "\n\n"
				+	"##########################################################");
		

		TASKS = WORKSPACE.resolve(fileService.restrictAccess(dbTasks, WORKSPACE));
		ASSETS_ORIGINAL = WORKSPACE.resolve(fileService.restrictAccess(assetsOriginal, WORKSPACE));
		ASSETS_NEW = WORKSPACE.resolve(fileService.restrictAccess(assetsNew, WORKSPACE));
		ASSET_PREVIEWS = WORKSPACE.resolve(fileService.restrictAccess(assetPreviews, WORKSPACE));
//		UPLOAD = wsAbsolute.resolve(fileService.restrictAccess(upload, wsAbsolute));
		USERS = WORKSPACE.resolve(fileService.restrictAccess(dbUsers, WORKSPACE));
		DB_OTHER = WORKSPACE.resolve(fileService.restrictAccess(dbOther, WORKSPACE));
		
		if (subNewTextures.isAbsolute() || subNewModels.isAbsolute() /*|| subNewWorlds.isAbsolute()*/) {
			throw new IllegalArgumentException("Asset type folder paths must be relative, not absolute!");
		}
		fileService.restrictAccess(subNewTextures, ASSETS_NEW);
		fileService.restrictAccess(subNewModels, ASSETS_NEW);
//		fileService.restrictAccess(subNewWorlds, ASSETS_NEW);
	}
	
	public Path assetsOriginal() {
		return ASSETS_ORIGINAL;
	}
	public Path assetsNew() {
		return ASSETS_NEW;
	}
	public Path assetPreviews() {
		return ASSET_PREVIEWS;
	}
	public Path assetsBase(AssetGroupType type) {
		return type.isOriginal() ? assetsOriginal() : assetsNew();
	}
	
	public Path dbUsers() {
		return USERS;
	}
	public Path dbTasks() {
		return TASKS;
	}
	public Path dbOther() {
		return DB_OTHER;
	}
//	public Path upload() {
//		return UPLOAD;
//	}

	public Path blender() {
		return BLENDER;
	}
	public Path blenderPythonScript() {
		return base.resolve("WEB-INF/python/gothic3dsToThree.py");
	}
	
	public Path subNewTextures() {
		return subNewTextures;
	}
	public Path subNewModels() {
		return subNewModels;
	}
//	public Path subNewWorlds() {
//		return subNewWorlds;
//	}

	public Path subAssets() {
		return subAssets;
	}
	public Path subOther() {
		return subOther;
	}
}
