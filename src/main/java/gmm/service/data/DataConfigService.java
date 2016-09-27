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

import gmm.service.FileService;


@Service
public class DataConfigService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final FileService fileService;
	
	private final Path base;
	
	@Value("${path.workspace}") private Path workspace;
	
	@Value("${path.assets.original}") private Path assetsOriginal;
	@Value("${path.assets.new}") private Path assetsNew;
	
	@Value("${path.users}") private Path dbUsers;
	@Value("${path.tasks}") private Path dbTasks;
	@Value("${path.other}") private Path dbOther;
	
	@Value("${path.upload}") private Path upload;
	
	@Value("${path.blender}") private Path blender;
	
	
	private Path USERS;
	private Path ASSETS_ORIGINAL;
	private Path ASSETS_NEW;
	private Path TASKS;
	private Path UPLOAD;
	private Path DB_OTHER;
	private Path BLENDER;
	
	private final Path SUB_ASSETS = Paths.get("assets");
	private final Path SUB_PREVIEW = Paths.get("preview");
	private final Path SUB_OTHER = Paths.get("wip");
	
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
				+	"  Expecting blender installation folder to be: " + "\n"
				+	"  " + blender + "\n\n"
				+	"##########################################################");
	}
	
	protected void updateWorkspace(Path workspace) {
		Objects.requireNonNull(workspace);
		final Path wsAbsolute = base.resolve(workspace).normalize();
		this.workspace = wsAbsolute;
		
		logger.info("\n"
				+	"##########################################################" + "\n\n"
				+	"  Registered workspace folder at: " + "\n"
				+	"  " + wsAbsolute + "\n\n"
				+	"##########################################################");
		
		TASKS = wsAbsolute.resolve(fileService.restrictAccess(dbTasks, wsAbsolute));
		ASSETS_ORIGINAL = wsAbsolute.resolve(fileService.restrictAccess(assetsOriginal, wsAbsolute));
		ASSETS_NEW = wsAbsolute.resolve(fileService.restrictAccess(assetsNew, wsAbsolute));
		UPLOAD = wsAbsolute.resolve(fileService.restrictAccess(upload, wsAbsolute));
		USERS = wsAbsolute.resolve(fileService.restrictAccess(dbUsers, wsAbsolute));
		DB_OTHER = wsAbsolute.resolve(fileService.restrictAccess(dbOther, wsAbsolute));
	}
	
	public Path assetsOriginal() {
		return ASSETS_ORIGINAL;
	}
	public Path assetsNew() {
		return ASSETS_NEW;
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
	public Path upload() {
		return UPLOAD;
	}

	public Path blender() {
		return BLENDER;
	}
	public Path blenderPythonScript() {
		return base.resolve("WEB-INF/python/gothic3dsToThree.py");
	}
	
	public Path subAssets() {
		return SUB_ASSETS;
	}
	public Path subPreview() {
		return SUB_PREVIEW;
	}
	public Path subOther() {
		return SUB_OTHER;
	}
}
