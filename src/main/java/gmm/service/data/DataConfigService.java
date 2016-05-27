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
	
	@Autowired private FileService fileService;
	@Autowired private ServletContext context;
	
	private Path base;
	
	@Value("${path.workspace}")
	private Path workspace;
	
	@Value("${path.users}")
	private Path users;
	
	@Value("${path.assets.original}")
	private Path assets_original;
	
	@Value("${path.assets.new}")
	private Path assets_new;
	
	@Value("${path.tasks}")
	private Path tasks;
	
	@Value("${path.other}")
	public Path dbOther;
	
	@Value("${path.upload}")
	public Path upload;
	
	@Value("${path.blender}")
	public Path blender;
	
	
	public Path USERS;
	public Path ASSETS_ORIGINAL;
	public Path ASSETS_NEW;
	public Path TASKS;
	public Path UPLOAD;
	public Path DB_OTHER;
	
	public final Path SUB_ASSETS = Paths.get("assets");
	public final Path SUB_PREVIEW = Paths.get("preview");
	public final Path SUB_OTHER = Paths.get("wip");
	
	@PostConstruct
	private void setUpConfigService() {
		base = Paths.get(context.getRealPath(""));
		updateWorkspace(workspace);
		blender = base.resolve(blender).normalize();
		logger.info("\n"
				+	"##########################################################" + "\n\n"
				+	"  Expecting blender installation folder to be: " + "\n"
				+	"  " + blender + "\n\n"
				+	"##########################################################");
	}
	
	public Path getWorkspace() {
		return workspace;
	}
	
	public Path getBlenderPythonScript() {
		return base.resolve("WEB-INF/python/gothic3dsToThree.py");
	}
	
	public void updateWorkspace(Path workspace) {
		Objects.requireNonNull(workspace);
		final Path wsAbsolute = base.resolve(workspace).normalize();
		this.workspace = wsAbsolute;
		
		logger.info("\n"
				+	"##########################################################" + "\n\n"
				+	"  Registered workspace folder at: " + "\n"
				+	"  " + wsAbsolute + "\n\n"
				+	"##########################################################");
		
		TASKS = 
				wsAbsolute.resolve(fileService.restrictAccess(tasks, wsAbsolute));
		ASSETS_ORIGINAL = 
				wsAbsolute.resolve(fileService.restrictAccess(assets_original, wsAbsolute));
		ASSETS_NEW =
				wsAbsolute.resolve(fileService.restrictAccess(assets_new, wsAbsolute));
		UPLOAD = 
				wsAbsolute.resolve(fileService.restrictAccess(upload, wsAbsolute));
		USERS = 
				wsAbsolute.resolve(fileService.restrictAccess(users, wsAbsolute));
		DB_OTHER =
				wsAbsolute.resolve(fileService.restrictAccess(dbOther, wsAbsolute));
	}
}
