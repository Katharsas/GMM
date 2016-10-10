package gmm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

public class TestConfig {

	public static Path testData = Paths.get("temp_testing");
	
	public static Path getTestFolderPath(Class<?> testClass) {
		return testData.resolve(testClass.getSimpleName()).toAbsolutePath();
	}
	
	public static void createTestFolder(Path testFolderPath) {
		if (!testFolderPath.toFile().isDirectory()) {
			if (!testFolderPath.toFile().mkdir()) {
				throw new IllegalStateException("Could not create test folder!");
			}
		} else {
			if (testFolderPath.toFile().list().length > 0) {
				throw new IllegalStateException("Test folder is not empty!");
			}
		}
	}
	
	public static void deleteTestFolder(Path testFolderPath) {
		if (!testFolderPath.toFile().delete()) {
			try {
				FileUtils.forceDelete(testFolderPath.toFile());
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
			throw new IllegalStateException("Test folder was not empty when deleted!");
		}
	}
}
