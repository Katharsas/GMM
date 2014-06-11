package gmm.service.data;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;






import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class DataConfigService {
	
	@Autowired private ServletContext context;
	
	@Value("${path.workspace}")
	private Path workspace;
	
	public Path USERS;
	
	@Value("${path.assets.original}")
	public Path ASSETS_ORIGINAL;
	
	@Value("${path.assets.new}")
	public Path ASSETS_NEW;
	
	@Value("${path.tasks}")
	public Path TASKS;
	
	@Value("${path.tasks.autobackup}")
	public Path TASKS_AUTO;
	
	@Value("${path.upload}")
	public Path UPLOAD;
	
	public final Path SUB_ASSETS = Paths.get("assets");
	public final Path SUB_PREVIEW = Paths.get("preview");
	public final Path SUB_OTHER = Paths.get("wip");
	
	@PostConstruct
	private void setUpConfigService() {
		Path base = Paths.get(context.getRealPath(""));
		Path wsAbsolute = base.resolve(workspace);
		workspace = wsAbsolute;
		
		System.out.println("##########################################################\n");
		System.out.println("  Registered workspace folder at: ");
		System.out.println("  " + workspace);
		System.out.println("\n##########################################################");
		
		TASKS = 			wsAbsolute.resolve(TASKS);
		TASKS_AUTO = 		wsAbsolute.resolve(TASKS_AUTO);
		ASSETS_ORIGINAL = 	wsAbsolute.resolve(ASSETS_ORIGINAL);
		ASSETS_NEW = 		wsAbsolute.resolve(ASSETS_NEW);
		UPLOAD = 			wsAbsolute.resolve(UPLOAD);
		USERS = 			wsAbsolute;
	}
}
