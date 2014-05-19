package gmm.service.data;

import java.io.FileInputStream;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class DataConfigService {
	
	@Autowired
	private String dataLocation;
	@Autowired
	private ServletContext context;
	private String basePath;
	
	public String DATA_USERS;
	public String DATA;
	public String DATA_AUTO;
	public String ASSETS_ORIGINAL;
	public String ASSETS_NEW;
	public String UPLOAD;
	
	public final String NEW_TEX_ASSETS = "assets";
	public final String NEW_TEX_PREVIEW = "preview";
	public final String NEW_TEX_OTHER = "wip";
	
	@PostConstruct
	private void setUpConfigService() {
		/**
		 * for Unit test environment
		 */
//		basePath = context.getRealPath("")+"/../../"+dataLocation;
		/**
		 * for server environment
		 */
		basePath = context.getRealPath("")+"/"+dataLocation;
		
		basePath = basePath.replace('\\', '/');
		Properties property = new Properties();
		try {
			property.load(new FileInputStream(basePath+"config.properties"));

		} catch (Exception/*IOException*/ e) {
			System.err.println("DataConfigService Error: Could not load config.properties file!");
			e.printStackTrace();
		}
		DATA = basePath+property.getProperty("data_backup_path");
		DATA_AUTO = basePath+property.getProperty("data_backup_automatic_path");
		ASSETS_ORIGINAL = basePath+property.getProperty("assets_original_path");
		ASSETS_NEW = basePath+property.getProperty("assets_new_path");
		UPLOAD = basePath+property.getProperty("upload_path");
		
		DATA_USERS = basePath;
	}
}
