package gmm.service.data;

import gmm.domain.Linkable;
import gmm.util.Collection;

public interface DataAccess {

	public <T extends Linkable> Collection<T> getList(Class<?> clazz);
	public <T extends Linkable> boolean addData(T data);
	public <T extends Linkable> boolean removeData(T data);
	public <T extends Linkable> boolean addAllData(Class<?> clazz, Collection<? extends T> data);
	public void removeAllData(Class<?> clazz);
	public void saveData(Class<?> clazz);
	public <T extends Linkable> void loadData(Class<?> clazz);
}
