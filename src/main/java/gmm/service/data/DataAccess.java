package gmm.service.data;

import gmm.domain.Linkable;
import gmm.util.Collection;

public interface DataAccess<T extends Linkable> {

	public Collection<T> getList(Class<?> clazz);
	public boolean addData(T data);
	public boolean removeData(T data);
	public boolean addAllData(Class<?> clazz, Collection<? extends T> data);
	public void removeAllData(Class<?> clazz);
	public void saveData(Class<?> clazz);
	public void loadData(Class<?> clazz);
}
