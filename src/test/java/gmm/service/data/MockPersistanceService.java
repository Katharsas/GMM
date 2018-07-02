package gmm.service.data;

import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MockPersistanceService implements PersistenceService {

	final Map<Path, Object> persistedObjects;
	final boolean simulateFilesReadWrite;
	
	public MockPersistanceService(boolean simulateFilesReadWrite) {
		this.simulateFilesReadWrite = simulateFilesReadWrite;
		persistedObjects = new HashMap<>();
	}
	
	@Override
	public synchronized void serialize(Object object, Path path) {
		persistedObjects.put(path.toAbsolutePath(), object);
		
		if (simulateFilesReadWrite) {
			try {
				Files.createFile(path);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Override
	public synchronized <T> T deserialize(Path path, Class<T> clazz) {
		if (simulateFilesReadWrite) {
			try (FileReader fileReader = new FileReader(path.toFile().getAbsolutePath())) {
				fileReader.read();
				fileReader.close();
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		@SuppressWarnings("unchecked")
		final T deserialzed = (T) persistedObjects.get(path.toAbsolutePath());
		if (deserialzed == null) {
			throw new IllegalArgumentException("Could not find file at given path!");
		}
		return deserialzed;
	}

}
