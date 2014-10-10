package gmm.service.filter;

import gmm.collections.Collection;

/**
 * Optimized version for gmm.collections, does not need copy method.
 * 
 * @author Jan Mothes
 */
public class GmmSelection<T, I extends Collection<T>> extends CustomSelection<T, I> {
	
	public GmmSelection(I elements, boolean selected) {
		super(elements, selected, null);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected I copy(I i) {
		return (I) i.copy();
	}
}
