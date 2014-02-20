package gmm.service.data;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import gmm.util.LinkedList;
import gmm.util.List;
import gmm.util.ListUtil;
import gmm.util.StringUtil;

public class DataBaseFilter<T> implements DataFilter<T> {
	private List<T> searchResults = new LinkedList<T>();
	private boolean ONLY_FILTER_EQUAL = false;
	
	public DataBaseFilter() {
	}
	
	public DataBaseFilter(LinkedList<T> searchResults) {
		this.searchResults = searchResults.clone();
	}
	
	@Override
	public void setOnlyFilterEqual(boolean onlyFilterEqual){
		ONLY_FILTER_EQUAL = onlyFilterEqual;
	}
	
	@Override
	public List<T> filterType(List<T> list, Class<T> filter) {
		List<T> buffer = list.clone();
		
		for(T t : buffer)	{
			Class<? extends Object> clazz = t.getClass();
			searchResults.add(t);
			if(clazz.equals(filter)) list.remove(t);
		}
		return list;
	}
	
	@Override
	public List<T> filterField(List<T> originalList, String getterMethodName, Object filter) {
		List<T> list = originalList.clone();
		List<T> buffer = list.clone();
		for (T t : buffer) {
			try {
				StringUtil.IGNORE_CASE=true;
				StringUtil.ALWAYS_CONTAINS_EMPTY=true;
				Method method = t.getClass().getMethod(getterMethodName);
				Object result = method.invoke(t);
				
				boolean bothNull = filter==null && result==null;
				boolean isNull = filter==null || result==null;
				boolean string = filter instanceof String && result instanceof String;
				boolean contains = string && (ONLY_FILTER_EQUAL ? 
						StringUtil.equals((String)result,(String)filter) : 
						StringUtil.contains((String)result,(String)filter));
				boolean equalsOrContains = !isNull && (contains || result.equals(filter)); 
				boolean equalsOrContainsOrToString = !isNull && (equalsOrContains || (ONLY_FILTER_EQUAL ?
						StringUtil.equals(result.toString(), filter.toString()) :
						StringUtil.contains(result.toString(), filter.toString())));
				if(bothNull || equalsOrContainsOrToString) {
					searchResults.add(t);
					list.remove(t);
				}
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				System.err.println("DataBaseFilter Error: Wrong getterMethodName or wrong list item!");
				System.err.println("GetterMethod must be implemented by all list items.");
				e.printStackTrace();
			}
		}
		return list;
	}
	
	@Override
	public List<T>filterAnd(List<T> originalList, String[] getterMethodNames, Object[] filters) {
		List<T> list = originalList.clone();
		List<T> buffer = list.clone();
		List<T> originalSearch = searchResults.clone();
		int size = getterMethodNames.length < filters.length ? getterMethodNames.length : filters.length;
		for(int i = 0; i<size; i++) {
			searchResults.clear();
			filterField(buffer, getterMethodNames[i], filters[i]);
			buffer = searchResults.clone();
		}
		for (T t : searchResults) {
			list.remove(t);
		}
		searchResults.addAll(originalSearch);
		return list;
	}
	
	@Override
	public List<T>filterOr(List<T> originalList, String[] getterMethodNames, Object[] filters) {
		List<T> list = originalList.clone();
		int size = getterMethodNames.length < filters.length ? getterMethodNames.length : filters.length;
		for(int i = 0; i<size; i++) {
			list = filterField(list, getterMethodNames[i], filters[i]);
		}
		return list;
	}

	@Override
	@Deprecated
	public List<T> filterTask(List<T> list, String filter) {
		return filterOr(list, new String[]{"getLabel","getDetails","getAuthor","getName"}, ListUtil.inflateToArray(filter, 4));
	}

	@Override
	public List<T> getFilteredElements() {
		return searchResults.clone();
	}

	@Override
	public void clear() {
		searchResults.clear();
	}
	
}
