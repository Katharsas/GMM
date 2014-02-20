package gmm.service.converters;
import org.springframework.context.ApplicationContext;

import gmm.domain.NamedObject;
import gmm.domain.User;
import gmm.service.ApplicationContextProvider;
import gmm.service.data.DataAccess;
import gmm.util.List;


public class UserReferenceConverter extends NamedReferenceConverter{
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean canConvert(Class clazz) {
		return clazz.equals(User.class);
	}

	@Override
	List<? extends NamedObject> getNamedObjects() {
		ApplicationContext context = ApplicationContextProvider.getApplicationContext();
		DataAccess data =  context.getBean(DataAccess.class);
		return data.getList(User.class);
	}
}
