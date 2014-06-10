package gmm.service.converters;
import org.springframework.context.ApplicationContext;

import gmm.collections.Collection;
import gmm.domain.NamedObject;
import gmm.domain.User;
import gmm.service.Spring;
import gmm.service.data.DataAccess;


public class UserReferenceConverter extends NamedReferenceConverter{
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean canConvert(Class clazz) {
		return clazz.equals(User.class);
	}

	@Override
	Collection<? extends NamedObject> getNamedObjects() {
		ApplicationContext context = Spring.getApplicationContext();
		DataAccess data =  context.getBean(DataAccess.class);
		return data.getList(User.class);
	}
}
