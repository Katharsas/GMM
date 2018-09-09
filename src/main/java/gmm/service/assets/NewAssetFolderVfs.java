package gmm.service.assets;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.jimfs.Jimfs;
import com.google.common.jimfs.UnixCompatible;
import com.google.common.jimfs.VPath;

import gmm.collections.EventMapSource;
import gmm.domain.task.asset.AssetName;
import gmm.service.FileService;

/**
 * Virtual file system that mirrors actual new asset directory (and updates on changes).
 * Asset folders are represented as empty files. Folders not containing asset folders are not included.
 * 
 * @author Jan Mothes
 */
@Service
public class NewAssetFolderVfs {

	private class VirtualPath extends VPath {
		public VirtualPath(String relativePath) {
			super(relativePath);
		}
		public VirtualPath(Path relativePath) {
			super(relativePath);
		}
		
		protected VirtualPath(Path path, boolean internal) {
			super(path, internal);
		}
		@Override
		protected Path convert(String relativePath) {
			return fileSystem.getPath(relativePath);
		}
		@Override
		protected VPath create(Path vPath) {
			return new VirtualPath(vPath, true);
		}
	}
	
	/**
	 * Converts paths to virtual paths with correct underlying fs.
	 */
	public class VirtualPaths {
		public final VPath root;
		private VirtualPaths() {
			this.root = new VirtualPath("/");
		}
		public VPath of(Path relativePath) {
			return new VirtualPath(relativePath);
		}
		public VPath of(String relativePath) {
			return new VirtualPath(relativePath);
		}
		public VPath root() {
			return new VirtualPath("/");
		}
//		public Path root() {
//			return fileSystem.getPath("/");
//		}
//		public Path get(Path path) {
//			return fileSystem.getPath(path.toString());
//		}
//		public Path get(String path) {
//			return fileSystem.getPath(path);
//		}
//		public Path getAsAbsolute(Path path) {
//			return fileSystem.getPath("/", path.toString());
//		}
//		public Path getAsAbsolute(String path) {
//			return fileSystem.getPath("/", path);
//		}
	}
	
	private final FileSystem fileSystem;
	private final VirtualPaths vPaths;
	
	private FileService fileService;
	
	@Autowired
	public NewAssetFolderVfs(AssetService assetService) {
		
		fileSystem = Jimfs.newFileSystem(UnixCompatible.config());
		vPaths = new VirtualPaths();
		
		try {
			Files.deleteIfExists(vPaths.root.resolve("work").get());
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		
		final EventMapSource<AssetName, NewAssetFolderInfo> eventSource = assetService.getNewAssetFoldersEvents();
		final Map<AssetName, NewAssetFolderInfo> current = eventSource.getLiveView();
		eventSource.register(this::onPut, this::onRemove);
		for (final Entry<AssetName, NewAssetFolderInfo> entry : current.entrySet()) {
			onPut(entry.getKey(), entry.getValue());
		}
	}
	
	public VirtualPaths virtualPaths() {
		return vPaths;
	}
	
	/**
	 * Checks if an asset folder could be created at given path. Makes sure that the folder does
	 * not exist already and that it is not inside another asset folder.
	 */
	public boolean isValidNewAssetFolderLocation(Path relative) {
		final Path vfsPath = vPaths.root.resolve(relative).get();
		if (Files.exists(vfsPath)) return false;
		try {
			fileService.testCreateDeleteFile(vfsPath);
			return true;
		} catch(final UncheckedIOException e) {
			return false;
		}
	}
	
	private void onPut(AssetName name, NewAssetFolderInfo info) {
		final Path localPath = info.getAssetFolder();
		if (localPath != null) {
			final Path vfsPath = vPaths.root.resolve(localPath).get();
			try {
				createFileAndParentFolders(vfsPath);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
	
	private void onRemove(AssetName name, NewAssetFolderInfo info) {
		final Path localPath = info.getAssetFolder();
		if (localPath != null) {
			final Path vfsPath = vPaths.root.resolve(localPath).get();
			if (Files.isRegularFile(vfsPath)) {
				try {
					deleteFileAndEmptyFolders(vfsPath);
				} catch (final IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
	}
	
	private synchronized void createFileAndParentFolders(Path path) throws IOException {
		Files.createDirectories(path.getParent());
		Files.createFile(path);
	}
	
	private synchronized void deleteFileAndEmptyFolders(Path path) throws IOException {
		Path current = path;
		Files.delete(current);
		current = current.getParent();
		while (isDirectoryEmpty(current) && !current.equals(vPaths.root)) {
			Files.delete(current);
			current = current.getParent();
		}
	}
	
	private static boolean isDirectoryEmpty(Path directory) throws IOException {
	    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
	        return !dirStream.iterator().hasNext();
	    }
	}
}
