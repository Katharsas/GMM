package gmm;

public class ConfigurationException extends RuntimeException {
	private static final long serialVersionUID = 1518219215454519776L;
	
	public ConfigurationException(String message) {
		super(message);
	}
	
	public ConfigurationException(Throwable cause) {
		super(cause);
	}
	
	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}
