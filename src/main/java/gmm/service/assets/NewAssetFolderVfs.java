package gmm.service.assets;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

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

	private final FileSystem fileSystem;
	private final Path root;
	
	private FileService fileService;
	
	@Autowired
	public NewAssetFolderVfs(AssetService assetService) {
		
		fileSystem = Jimfs.newFileSystem(Configuration.unix());
		root = fileSystem.getPath("/");
		
		try {
			Files.deleteIfExists(root.resolve("work"));
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
	
	public Path getVirtualRootAssetsNew() {
		return root;
	}
	
	public Path convertRelativePathToVfs(Path relative) {
		return root.resolve(FilenameUtils.separatorsToUnix(relative.toString()));
	}
	
	/**
	 * Checks if an asset folder could be created at given path. Makes sure that the folder does
	 * not exist already and that it is not inside another asset folder.
	 */
	public boolean isValidNewAssetFolderLocation(Path relative) {
		final Path vfsRelative = convertRelativePathToVfs(relative);
		if (Files.exists(vfsRelative)) return false;
		try {
			fileService.testCreateDeleteFile(vfsRelative);
			return true;
		} catch(final UncheckedIOException e) {
			return false;
		}
	}
	
	private void onPut(AssetName name, NewAssetFolderInfo info) {
		final Path localPath = info.getAssetFolder();
		if (localPath != null) {
			final Path unixPath = convertRelativePathToVfs(localPath);
			try {
				createFileAndParentFolders(unixPath);
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
	
	private void onRemove(AssetName name, NewAssetFolderInfo info) {
		final Path localPath = info.getAssetFolder();
		if (localPath != null) {
			final Path unixPath = convertRelativePathToVfs(localPath);
			if (Files.isRegularFile(unixPath)) {
				try {
					deleteFileAndEmptyFolders(unixPath);
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
		while (isDirectoryEmpty(current) && !current.equals(root)) {
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
