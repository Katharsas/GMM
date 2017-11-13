package gmm.service.data.xstream;
import java.util.function.Supplier;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import gmm.collections.Collection;
import gmm.domain.UniqueObject;
import gmm.domain.User;

public class UserReferenceConverter extends IdReferenceConverter {
	
	private final Supplier<Collection<User>> users;
	
	public UserReferenceConverter(Supplier<Collection<User>> users) {
		this.users = users;
	}
	
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
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		final Object user = super.unmarshal(reader, context);
		return user == null ? User.NULL : user;
	}

	@Override
	Collection<User> getUniqueObjects() {
		return users.get();
	}
}
