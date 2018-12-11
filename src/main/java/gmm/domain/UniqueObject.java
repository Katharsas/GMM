package gmm.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.Locale;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public abstract class UniqueObject implements Linkable {
	
	//Variables-------------------------------------------------
	private static long idCount = 0;
	@XStreamAsAttribute
	private long id;
	private final Instant created;
	
	//Methods---------------------------------------------------
	public UniqueObject() {
		id = ++idCount;
		this.created = Instant.now();
	}
	
	@Override
	public String getIdLink() {
		return getClass().getSimpleName()+id;
	}
	
	/**
	 * @return null if an element with this idLink does not exist in the given collection
	 */
	public  static <U extends UniqueObject> U getFromIdLink(Collection<U> c, String idLink) {
		for(final U u : c) {
			if(u.getIdLink().equals(idLink)) return u;
		}
		return null;
	}
	
	/**
	 * @return null if an element with this id does not exist in the given collection
	 */
	public  static <U extends UniqueObject> U getFromId(Collection<U> c, long id) {
		for(final U u : c) {
			if(u.getId() == id) return u;
		}
		return null;
	}
	
	public  static <U extends UniqueObject> void updateCounter(Collection<U> c) {
		for(final U u : c) {
			updateCounter(u);
		}
	}
	
	public static <U extends UniqueObject> void updateCounter(U u) {
		if(u.getId() > idCount) idCount = u.getId();
	}
	
	public <U extends UniqueObject> void makeUnique() {
		id = ++idCount;
	}
	
	public long getId() {
		return id;
	}
	
	@Override
	public int hashCode() {
		return getIdLink().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if((o!=null) && o instanceof UniqueObject){
			final UniqueObject p = (UniqueObject) o;
    		return getIdLink().equals(p.getIdLink());
    	}
    	return false;
	}
	

	//Setters, Getters----------------------------------------------
	public Instant getCreationDate() {
		return created;
	}
	
	public String getFormattedCreationDate(Locale locale) {
		final DateTimeFormatter  formatter = DateTimeFormatter
				.ofLocalizedDate(FormatStyle.MEDIUM)
				.withLocale(locale);
		final LocalDate localDate = created.atZone(ZoneOffset.UTC).toLocalDate();
		return formatter.format(localDate);
	}
}
