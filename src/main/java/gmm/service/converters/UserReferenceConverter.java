package gmm.service.converters;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import gmm.collections.Collection;
import gmm.domain.UniqueObject;
import gmm.domain.User;
import gmm.service.Spring;
import gmm.service.data.DataAccess;


public class UserReferenceConverter extends IdReferenceConverter{
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean canConvert(Class clazz) {
		return clazz.equals(User.class);
	}
	
	@Override
	protected void addAttributes(HierarchicalStreamWriter writer, UniqueObject source) {
		super.addAttributes(writer, source);
		final User user = (User) source;
		writer.addAttribute("name", user.getName());
	}

	@Override
	Collection<? extends User> getUniqueObjects() {
		final DataAccess data =  Spring.get(DataAccess.class);
		return data.getList(User.class);
	}
}
