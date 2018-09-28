package gmm.service.data;

public class MockConfig extends Config {

	public MockConfig(PathConfig pathConfig) {
		super(pathConfig);
	}
	
	@Override
	public int getPreviewThreadCount() {
		return 8;
	}
}
