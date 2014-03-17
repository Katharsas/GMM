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
	
	public String DATA_CURRENT;
	public String DATA_BACKUP;
	
	public String ASSETS_ORIGINAL;
	public String ASSETS_NEW;
	public String ASSETS_NEW_BACKUP;
	public String UPLOAD;
	
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
		DATA_CURRENT = basePath+property.getProperty("database_current_path");
		DATA_BACKUP = basePath+property.getProperty("database_autobackup_path");
		ASSETS_ORIGINAL = basePath+property.getProperty("filedirectory_original_path");
		ASSETS_NEW = basePath+property.getProperty("filedirectory_new_path");
		ASSETS_NEW_BACKUP = basePath+property.getProperty("filedirectory_new_autobackup_path");
		UPLOAD = basePath+property.getProperty("filedirectory_upload_path");
		
	}
}
