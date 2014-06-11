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
	private String workspace;
	
	public String USERS;
	
	@Value("${path.assets.original}")
	public String ASSETS_ORIGINAL;
	
	@Value("${path.assets.new}")
	public String ASSETS_NEW;
	
	@Value("${path.tasks}")
	public String TASKS;
	
	@Value("${path.tasks.autobackup}")
	public String TASKS_AUTO;
	
	@Value("${path.upload}")
	public String UPLOAD;
	
	public final String SUB_ASSETS = "assets";
	public final String SUB_PREVIEW = "preview";
	public final String SUB_OTHER = "wip";
	
	@PostConstruct
	private void setUpConfigService() {
		Path base = Paths.get(context.getRealPath(""));
		Path w = base.resolve(workspace);
		workspace = w.toString();
		
		System.out.println("##########################################################\n");
		System.out.println("  Registered workspace folder at: ");
		System.out.println("  " + workspace);
		System.out.println("\n##########################################################");
		
		TASKS = w.resolve(TASKS).toString();
		TASKS_AUTO = w.resolve(TASKS_AUTO).toString();
		ASSETS_ORIGINAL = w.resolve(ASSETS_ORIGINAL).toString();
		ASSETS_NEW = w.resolve(ASSETS_NEW).toString();
		UPLOAD = w.resolve(UPLOAD).toString();
		USERS = w.resolve(workspace).toString();
	}
}
