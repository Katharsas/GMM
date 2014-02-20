package gmm.domain;

import java.util.LinkedList;
import java.util.List;

public class User extends NamedObject {
	
	//Variables-------------------------------------------
	//Domain - Set by constructor
	private String passwordHash;
	//Domain - Default
	private boolean isAdmin=false;
	private String email="";
	final private List<Notification> oldNotifications = new LinkedList<Notification>();;
	final private List<Notification> newNotifications = new LinkedList<Notification>();
	
	//Options
	public boolean sentNotificationsToMail = false;
	public int daysToSaveNotifiations = 30;
	public int maximumSavedNotifications = 100;
	
	//Methods--------------------------------------------
	/**
	 * @param idName - Identificator (name) of the user.
	 * @param passwordHash - The users password hash.
	 */
	public User(String idName, String passwordHash) {
		super(idName);
		if (passwordHash==null) throw new NullPointerException();
		this.passwordHash = passwordHash;
	}
	
	//Setters, Getters---------------------------------------
	public void setPasswordHash(String passwordHash) {
		if (passwordHash==null) throw new NullPointerException();
		this.passwordHash = passwordHash;
	}
	public String getPasswordHash() {
		return passwordHash;
	}

	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	public boolean isAdmin() {
		return isAdmin;
	}

	public void setEmail(String email) {
		if (email==null) throw new NullPointerException();
		this.email = email;
	}
	public String getEmail() {
		return email;
	}
	
	public List<Notification> getOldNotifications() {
		return oldNotifications;
	}
	public List<Notification> getNewNotifications() {
		return newNotifications;
	}

	public String getName() {
		return getIdName();
	}

	public void setName(String name) {
		setIdName(name);
	}
}
