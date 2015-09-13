package gmm.service.converters;
import org.springframework.context.ApplicationContext;

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
		User user = (User) source;
		writer.addAttribute("name", user.getName());
	}

	@Override
	Collection<? extends User> getUniqueObjects() {
		ApplicationContext context = Spring.getApplicationContext();
		DataAccess data =  context.getBean(DataAccess.class);
		return data.getList(User.class);
	}
}
