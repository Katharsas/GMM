package gmm.service.data;

import gmm.util.Collection;

public interface DataAccess {

	public <T> Collection<T> getList(Class<T> clazz);
	public <T> boolean addData(T data);
	public <T> boolean removeData(T data);
	public <T> boolean addAllData(Class<T> clazz, Collection<? extends T> data);
	public void removeAllData(Class<?> clazz);
	public void saveData(Class<?> clazz);
	public <T> void loadData(Class<T> clazz);
}
