package gmm.service.ajax;

import java.util.Objects;

public class MessageResponse {
	private String status;
	private String message;
	
	public MessageResponse(String status, String message) {
		Objects.requireNonNull(status);
		this.status = status;
		this.message = message;
	}

	@Override
	public String toString() {
		return "[ Status: "+status+", "+"Message: "+message+" ]";
	}
	
	public String getStatus() {
		return status;
	}
	
	public String getMessage() {
		return message;
	}
}