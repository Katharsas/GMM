package gmm.service.data;

import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.Linkable;

@Service
public interface DataAccess {

	public <T extends Linkable> Collection<T> getList(Class<T> clazz);
	public <T extends Linkable> boolean add(T data);
	public <T extends Linkable> boolean remove(T data);
	public <T extends Linkable> boolean addAll(Class<T> clazz, Collection<? extends T> data);
	public <T extends Linkable> void removeAll(Class<T> clazz);
	public <T extends Linkable> void removeAll(Collection<T> data);
	public boolean hasIds(long[] id);
	public CombinedData getCombinedData();
}
