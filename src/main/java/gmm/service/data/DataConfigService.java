package gmm.service.data;

import gmm.service.FileService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;








import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class DataConfigService {
	
	@Autowired private FileService fileService;
	@Autowired private ServletContext context;
	
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
	
	@Value("${path.upload}")
	public Path upload;
	
	
	public Path USERS;
	public Path ASSETS_ORIGINAL;
	public Path ASSETS_NEW;
	public Path TASKS;
	public Path UPLOAD;
	
	public final Path SUB_ASSETS = Paths.get("assets");
	public final Path SUB_PREVIEW = Paths.get("preview");
	public final Path SUB_OTHER = Paths.get("wip");
	
	@PostConstruct
	private void setUpConfigService() {
		updateWorkspace(workspace);
	}
	
	public Path getWorkspace() {
		return workspace;
	}
	
	public void updateWorkspace(Path workspace) {
		Objects.requireNonNull(workspace);
		this.workspace = workspace;
		
		Path base = Paths.get(context.getRealPath(""));
		Path wsAbsolute = base.resolve(workspace);
		workspace = wsAbsolute;
		
		System.out.println("##########################################################\n");
		System.out.println("  Registered workspace folder at: ");
		System.out.println("  " + wsAbsolute);
		System.out.println("\n##########################################################");
		
		TASKS = 			wsAbsolute.resolve(fileService.restrictAccess(tasks, wsAbsolute));
		ASSETS_ORIGINAL = 	wsAbsolute.resolve(fileService.restrictAccess(assets_original, wsAbsolute));
		ASSETS_NEW = 		wsAbsolute.resolve(fileService.restrictAccess(assets_new, wsAbsolute));
		UPLOAD = 			wsAbsolute.resolve(fileService.restrictAccess(upload, wsAbsolute));
		USERS = 			wsAbsolute.resolve(fileService.restrictAccess(users, wsAbsolute));
	}
}
