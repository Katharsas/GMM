package gmm.service.data.xstream;
import java.util.function.Supplier;

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
	Collection<User> getUniqueObjects() {
		return users.get();
	}
}
