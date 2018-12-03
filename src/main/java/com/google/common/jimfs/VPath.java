package com.google.common.jimfs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

/**
 * Allows to combine paths from different FileSystem. If arguments contain paths
 * with different FileSystem, they are converted to paths with this fileSystem.
 * This obviously only works for relative paths.
 */
public abstract class VPath {
	
	/**
	 * Factory & convenience class for "virtual" {@link VPath} similar to {@link Paths}.
	 */
	public static class VPaths {
		
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
				return fs.getPath(relativePath);
			}
			@Override
			protected VPath create(Path vPath) {
				return new VirtualPath(vPath, true);
			}
		}
		
		private final FileSystem fs;
		public final VPath root;
		
		public VPaths(FileSystem fs, String root) {
			this.fs = fs;
			this.root = new VirtualPath("/");
		}
		
		public VPath of(Path relativePath) {
			return new VirtualPath(relativePath);
		}
		public VPath of(String relativePath) {
			return new VirtualPath(relativePath);
		}
	}
	
	
	private boolean equalsFS(Path other) {
		return path.getFileSystem().equals(other.getFileSystem());
	}
	
	private final Path path;
	
	public VPath(String relativePath) {
		this.path = convert(relativePath);
	}
	public VPath(Path relativePath) {
		this.path = convert(relativePath);
	}
	protected VPath(Path path, boolean internal) {
		this.path = path;
	}
	
	public Path get() {
		return path;
	}
	
	protected Path convert(Path relativePath) {
		return convert(relativePath.toString());
	}
	protected abstract Path convert(String relativePath);
	protected abstract VPath create(Path vPath);
	
	
	public VPath resolve(String other) {
		return create(path.resolve(other));
	}
	
	public VPath resolve(Path other) {
		return create(path.resolve(
				equalsFS(other) ? other : convert(other)));
	}
	
	public FileSystem getFileSystem() {
		return path.getFileSystem();
	}
	
	public boolean isAbsolute() {
		return path.isAbsolute();
	}
	
	public VPath getRoot() {
		return create(path.getRoot());
	}
	
	public VPath getFileName() {
		return create(path.getFileName());
	}
	
	public VPath getParent() {
		return create(path.getParent());
	}
	
	public int getNameCount() {
		return path.getNameCount();
	}
	
	public VPath getName(int index) {
		return create(path.getName(index));
	}
	
	public VPath subpath(int beginIndex, int endIndex) {
		return create(path.subpath(beginIndex, endIndex));
	}
	
	public boolean startsWith(Path other) {
		return path.startsWith(equalsFS(other) ? other : convert(other));
	}
	
	public boolean startsWith(String other) {
		return path.startsWith(other);
	}
	
	public boolean endsWith(Path other) {
		return path.endsWith(equalsFS(other) ? other : convert(other));
	}
	
	public boolean endsWith(String other) {
		return path.endsWith(other);
	}
	
	public VPath normalize() {
		return create(path.normalize());
	}
	
	public VPath resolveSibling(Path other) {
		return create(path.resolveSibling(equalsFS(other) ? other : convert(other)));
	}
	
	public VPath resolveSibling(String other) {
		return create(path.resolveSibling(other));
	}
	
	public VPath relativize(Path other) {
		return create(path.relativize(equalsFS(other) ? other : convert(other)));
	}
	
	public URI toUri() {
		return path.toUri();
	}
	
	public VPath toAbsolutePath() {
		return create(path.toAbsolutePath());
	}
	
	public VPath toRealPath(LinkOption... options) throws IOException {
		return create(path.toRealPath(options));
	}
	
	public File toFile() {
		return path.toFile();
	}
	
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		return path.register(watcher, events, modifiers);
	}
	
	public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
		return path.register(watcher, events);
	}
	
	public Iterator<Path> iterator() {
		return path.iterator();
	}
	
	public int compareTo(Path other) {
		return path.compareTo(other);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		final Path other;
		if (obj instanceof VPath) {
			other = ((VPath) obj).path;
		}
		else if (obj instanceof Path) {
			other = (Path) obj;
		}
		else {
			return false;
		}
		if (path.isAbsolute() || other.isAbsolute() || equalsFS(other)) {
			return path.equals(other);
		} else {
			final Path otherConverted = convert(other);
			return path.equals(otherConverted);
		}
	}
	
	@Override
	public int hashCode() {
		return path.hashCode();
	}
	
	@Override
	public String toString() {
		return path.toString();
	}
}