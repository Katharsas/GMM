package gmm;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestConfig {

	public static Path testData = Paths.get("temp_testing");
	public static Path getTestFolderPath(Class<?> testClass) {
		return testData.resolve(testClass.getSimpleName());
	}
}
