package gmm.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.List;
import gmm.service.assets.NewAssetFolderInfo;

public class VirtualNewAssetFileSystem {
	
	private final Collection<NewAssetFolderInfo> assetFoldersWithoutTasks;
	private FileSystem fileSystem;
	private Path root;
	
	public VirtualNewAssetFileSystem(Collection<NewAssetFolderInfo> folderInfoLiveView)  {
		this.assetFoldersWithoutTasks = folderInfoLiveView;
		update();
	}
	
	public void update() {
		try {
			fileSystem = Jimfs.newFileSystem(Configuration.unix());
			root = fileSystem.getPath("/");
			
			Files.deleteIfExists(root.resolve("work"));
			
			final List<Path> assetFoldersConverted = new ArrayList<>(Path.class, assetFoldersWithoutTasks.size());
			for (final NewAssetFolderInfo info : assetFoldersWithoutTasks) {
				final Path localPath = info.getAssetFolder();
				final Path unixPath = convertRelativePathToVfsPath(localPath);
				assetFoldersConverted.add(unixPath);
			}
			
			for (final Path assetFolder : assetFoldersConverted) {
				Files.createDirectories(assetFolder.getParent());
				Files.createFile(assetFolder);
			}
			
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public Path getVirtualRootAssetsNew() {
		return root;
	}
	
	public Path convertRelativePathToVfsPath(Path relative) {
		return root.resolve(FilenameUtils.separatorsToUnix(relative.toString()));
	}
}
