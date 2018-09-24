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

import com.google.common.jimfs.GothicCompatible;
import com.google.common.jimfs.Jimfs;
import com.google.common.jimfs.VPath;

import gmm.collections.EventMapSource;
import gmm.domain.task.asset.AssetKey;
import gmm.service.FileService;

/**
 * Virtual file system that mirrors actual new asset directory (and updates on changes).
 * Asset folders are represented as empty files. Folders not containing asset folders
 * are not included. All path & file equality checks are case-insensitive.
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
	}
	
	private final FileSystem fileSystem;
	private final VirtualPaths vPaths;
	
	private final FileService fileService;
	
	@Autowired
	public NewAssetFolderVfs(AssetService assetService, FileService fileService) {
		this.fileService = fileService;
		fileSystem = Jimfs.newFileSystem(GothicCompatible.config());
		vPaths = new VirtualPaths();
		
		try {
			Files.deleteIfExists(vPaths.root.resolve("work").get());
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		
		final EventMapSource<AssetKey, NewAssetFolderInfo> eventSource = assetService.getNewAssetFoldersEvents();
		final Map<AssetKey, NewAssetFolderInfo> current = eventSource.getLiveView();
		eventSource.register(this::onPut, this::onRemove);
		for (final Entry<AssetKey, NewAssetFolderInfo> entry : current.entrySet()) {
			onPut(entry.getKey(), entry.getValue());
		}
	}
	
	public VirtualPaths virtualPaths() {
		return vPaths;
	}
	
	/**
	 * Makes sure that the folder does not exist already and that it is not inside another asset folder.
	 */
	public boolean isValidAssetFolderLocation(Path relative) {
		final Path vfsPath = vPaths.root.resolve(relative).get();
		if (Files.exists(vfsPath)) return false;
		try {
			fileService.testCreateDeleteFile(vfsPath);
			return true;
		} catch(final UncheckedIOException e) {
			return false;
		}
	}
	
	/**
	 * Inserts asset folder represented as file into VFS.
	 */
	private void onPut(AssetKey __, NewAssetFolderInfo info) {
		final Path localPath = info.getAssetFolder();
		if (localPath != null) {
			final Path vfsPath = vPaths.root.resolve(localPath).get();
			try {
				if (Files.isRegularFile(vfsPath)) {
					Files.delete(vfsPath);
				}
				createFileAndParentFolders(vfsPath);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
	
	/**
	 * Removed asset folder (which is represented as file) from VFS.
	 */
	private void onRemove(AssetKey __, NewAssetFolderInfo info) {
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
		while (isDirectoryEmpty(current) && !current.equals(vPaths.root.get())) {
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
