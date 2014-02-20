package gmm.service.data;

import gmm.util.List;

public interface DataAccess {
	public <T> List<T> getList(Class<T> clazz);
	public <T> boolean addData(T data);
	public <T> boolean removeData(T data);
	public <T> boolean addAllData(List<T> data);
	public <T> void removeAllData(Class<T> clazz);
	public void saveData(Class<?> clazz);
	public <T> void loadData(Class<T> clazz);
}
