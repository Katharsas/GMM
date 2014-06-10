package gmm.service.data;

import gmm.collections.Collection;
import gmm.domain.Linkable;

public interface DataAccess {

	public <T extends Linkable> Collection<T> getList(Class<?> clazz);
	public <T extends Linkable> boolean add(T data);
	public <T extends Linkable> boolean remove(T data);
	public <T extends Linkable> boolean addAll(Class<?> clazz, Collection<? extends T> data);
	public void removeAll(Class<?> clazz);
}
