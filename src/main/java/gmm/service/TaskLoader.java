package gmm.service;

import gmm.domain.Task;
import gmm.service.data.DataAccess;
import gmm.service.data.XMLSerializerService;
import gmm.util.Collection;

import java.util.Iterator;

public class TaskLoader {
	
	DataAccess data = 
			ApplicationContextProvider.getService(DataAccess.class);
	XMLSerializerService xmlService = 
			ApplicationContextProvider.getService(XMLSerializerService.class);
	
	private Iterator<? extends Task> taskLoader;
	private Task currentlyLoaded;
	private boolean skipAll = false;
	private boolean overwriteAll = false;
	private boolean bothAll = false;
	
	public TaskLoader(String path) {
		taskLoader = ((Collection<? extends Task>) xmlService.deserialize(path)).iterator();
	}
	
	
	public String loadNext(String operation, boolean doForAll) {
		switch(operation) {
		case "skip":
			//TODO
			break;
		case "overwrite":
			//TODO
			break;
		case "both":
			//TODO
			break;
		default:
			break;
		}
		if (!taskLoader.hasNext()) {
			return "{\"status\": \"finished\"}";
		}
		currentlyLoaded = taskLoader.next();
		System.out.println("Loading Task with name "+currentlyLoaded.getName());
		boolean success = data.addData(currentlyLoaded);
		String result = "{" +
				"\"status\": \""+ (success ? "success" : "conflict") +"\",\n" +
				"\"element\": \""+currentlyLoaded.getName()+"\"}";
		System.out.println("Returning JSON:\n"+result);
		return result;
	}
}
