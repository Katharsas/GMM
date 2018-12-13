package gmm.service.data;

public class MockDataBaseInitNotifier extends DataBaseInitNotifier {
	
	@Override
	public boolean isInitDone() {
		return true;
	}
}
