package com.google.common.jimfs;

import static com.google.common.jimfs.Feature.FILE_CHANNEL;
import static com.google.common.jimfs.Feature.LINKS;
import static com.google.common.jimfs.Feature.SECURE_DIRECTORY_STREAM;
import static com.google.common.jimfs.Feature.SYMBOLIC_LINKS;

import java.nio.file.InvalidPathException;

/**
 * Very similar to normal Configuration.unix(), but is able to parse windows separators in
 * addition to unix separator and is case-insensitive which allows to check for conflicts
 * due to case-insensitivity.
 * 
 * @author Jan Mothes
 */
public class GothicCompatible {
	
	public static Configuration config() {
		return Configuration.builder(new CompatiblePathType())
	            .setRoots("/")
	            .setWorkingDirectory("/work")
	            .setAttributeViews("basic")
	            .setSupportedFeatures(LINKS, SYMBOLIC_LINKS, SECURE_DIRECTORY_STREAM, FILE_CHANNEL)
	            .setNameCanonicalNormalization(PathNormalization.CASE_FOLD_ASCII)
	            .build();
	}
	
	static class CompatiblePathType extends PathType {

		final PathType unix;
		
		public CompatiblePathType() {
			super(false, '/', '\\');
			unix = PathType.unix();
		}

		@Override
		public ParseResult parsePath(String path) {
			if (path.isEmpty()) {
				return emptyPath();
			}

			checkValid(path);

			final String root = path.startsWith("/") ? "/" : null;
			return new ParseResult(root, splitter().split(path));
		}
		
		private static void checkValid(String path) {
			final int nulIndex = path.indexOf('\0');
			if (nulIndex != -1) {
				throw new InvalidPathException(path, "nul character not allowed", nulIndex);
			}
		}

		@Override
		public String toString(String root, Iterable<String> names) {
			return unix.toString(root, names);
		}

		@Override
		public String toString() {
			return unix.toString();
		}

		@Override
		protected String toUriPath(String root, Iterable<String> names, boolean directory) {
			return unix.toUriPath(root, names, directory);
		}

		@Override
		protected ParseResult parseUriPath(String uriPath) {
			return unix.parseUriPath(uriPath);
		}
	}
}


