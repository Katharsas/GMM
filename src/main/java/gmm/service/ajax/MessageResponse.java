package gmm.service.ajax;

public class MessageResponse {
	public String status;
	public String message;
	@Override
	public String toString() {
		return "Status: "+status+", "+"Message: "+message;
	}
}