package gmm.web.sessions;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.Task;
import gmm.domain.UniqueObject;
import gmm.service.data.DataAccess;

@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class LinkSession {

	@Autowired private DataAccess data;
	
	List<Task> tasks = new LinkedList<>();
	
	public void setTaskLinks(String ids) {
		tasks.clear();
		String[] idArray = ids.split(",");
		Collection<Task> allTasks = data.getList(Task.class);
		for (String id : Arrays.asList(idArray)) {
			Task task = UniqueObject.getFromId(allTasks, Long.parseLong(id));
			if (task != null) tasks.add(task);
		}
	}
	
	public List<Task> getTaskLinks() {
		return tasks;
	}
}
