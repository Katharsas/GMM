package gmm.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import gmm.TestConfig;
import gmm.collections.ArrayList;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.service.FileService.PathFilter;

/**
 * Note:
 * java.nio.Path#normalize() will reduce full stop ("." = same directory), to empty string ("").
 * So test for empty string instead of full stop or normalize your full stops.
 * 
 * TODO test null / invalid args
 * 
 * @author Jan Mothes
 */
public class FileServiceTest {

	private static FileService fileService;
	private static Path testPath;

	@BeforeClass
	public static void init() {
		fileService = new FileService();
		testPath = TestConfig.getTestFolderPath(FileServiceTest.class);
		TestConfig.createTestFolder(testPath);
	}
	
	@AfterClass
	public static void clean() {
		TestConfig.deleteTestFolder(testPath);
	}
	
	@Test
	public void testPathStartsWith() {
		final Path base = Paths.get("/folder");
		final Path sub1 = Paths.get("/folder/sub");
		final Path sub2 = Paths.get("/other/../folder/sub").normalize();
		final Path sub3 = Paths.get("/folder/other/../sub").normalize();
		final Path wrongSub = Paths.get("/folder/sub/../../other").normalize();
		
		assertTrue(sub1.startsWith(base));
		assertTrue(sub2.startsWith(base));
		assertTrue(sub3.startsWith(base));
		assertFalse(wrongSub.startsWith(base));
	}
	
	@Test
	public void testRestrictAccess() {
		final Path visible = Paths.get("/folder/visible");
		
		// absolute forbidden paths
		final Path subAbsForbidden1 = Paths.get("/folder");
		final Path subAbsForbidden2 = Paths.get("/folder/visible/../");
		final Path subAbsForbidden3 = Paths.get("/folder/invisible");
		
		// relative forbidden paths
		final Path subRelForbidden1 = Paths.get("..");
		final Path subRelForbidden2 = Paths.get("../invisible");
		final Path subRelForbidden3 = Paths.get("sub/../../invisible");
		
		// absolute allowed paths
		final Path subAbsAllowed1 = Paths.get("/folder/visible");
		final Path subAbsAllowed2 = Paths.get("/folder/visible/sub/..");
		final Path subAbsAllowed3 = Paths.get("/folder/visible/sub");
		
		// relative allowed paths
		final Path subRelAllowed1 = Paths.get(".");
		final Path subRelAllowed2 = Paths.get("sub/..");
		final Path subRelAllowed3 = Paths.get("sub");
		
		// when forbidden, throw exception
		
		try{fileService.restrictAccess(subAbsForbidden1, visible);fail();}
		catch(final IllegalArgumentException e){}
		try{fileService.restrictAccess(subAbsForbidden2, visible);fail();}
		catch(final IllegalArgumentException e){}
		try{fileService.restrictAccess(subAbsForbidden3, visible);fail();}
		catch(final IllegalArgumentException e){}
		
		try{fileService.restrictAccess(subRelForbidden1, visible);fail();}
		catch(final IllegalArgumentException e){}
		try{fileService.restrictAccess(subRelForbidden2, visible);fail();}
		catch(final IllegalArgumentException e){}
		try{fileService.restrictAccess(subRelForbidden3, visible);fail();}
		catch(final IllegalArgumentException e){}
		
		// otherwise return path relative to visible
		
		final Path subAbsAllowed1Answer = fileService.restrictAccess(subAbsAllowed1, visible);
		final Path subAbsAllowed2Answer = fileService.restrictAccess(subAbsAllowed2, visible);
		final Path subAbsAllowed3Answer = fileService.restrictAccess(subAbsAllowed3, visible);
		assertEquals(Paths.get(""), subAbsAllowed1Answer.normalize());
		assertEquals(Paths.get(""), subAbsAllowed2Answer.normalize());
		assertEquals(Paths.get("sub"), subAbsAllowed3Answer.normalize());
		
		final Path subRelAllowed1Answer = fileService.restrictAccess(subRelAllowed1, visible);
		final Path subRelAllowed2Answer = fileService.restrictAccess(subRelAllowed2, visible);
		final Path subRelAllowed3Answer = fileService.restrictAccess(subRelAllowed3, visible);
		assertEquals(Paths.get(""), subRelAllowed1Answer.normalize());
		assertEquals(Paths.get(""), subRelAllowed2Answer.normalize());
		assertEquals(Paths.get("sub"), subRelAllowed3Answer.normalize());
	}
	
	@Test
	public void testGetRelativeNames() {
		final Path visible = testPath.resolve("/folder/relative");
		ArrayList<String> relatives;
		ArrayList<Path> paths;
		
		// allowed inputs
		
		paths = new ArrayList<>(Path.class);
		paths.add(testPath.resolve("/folder"));
		paths.add(testPath.resolve("/folder/relative"));
		paths.add(testPath.resolve("/folder/relative/sub/.."));
		paths.add(testPath.resolve("/folder/relative/sub"));
		paths.add(testPath.resolve("/folder/relative/sub/sub"));
		
		relatives = (ArrayList<String>) fileService.getRelativeNames(paths, visible);
		assertEquals(Paths.get(".."), Paths.get(relatives.get(0)).normalize());
		assertEquals(Paths.get(""), Paths.get(relatives.get(1)).normalize());
		assertEquals(Paths.get(""), Paths.get(relatives.get(2)).normalize());
		assertEquals(Paths.get("sub"), Paths.get(relatives.get(3)).normalize());
		assertEquals(Paths.get("sub/sub"), Paths.get(relatives.get(4)).normalize());
		
		// wrong inputs (non-absolute paths)
		
		paths = new ArrayList<>(Path.class);
		paths.add(Paths.get("folder/relative"));
		try{fileService.getRelativeNames(paths, visible);fail();}
		catch(final IllegalArgumentException e){}
		
		paths = new ArrayList<>(Path.class);
		paths.add(Paths.get("relative"));
		try{fileService.getRelativeNames(paths, visible);fail();}
		catch(final IllegalArgumentException e){}
		
		paths = new ArrayList<>(Path.class);
		paths.add(testPath.resolve("/folder/relative"));
		try{fileService.getRelativeNames(paths, Paths.get("folder/relative"));fail();}
		catch(final IllegalArgumentException e){}
		
		paths = new ArrayList<>(Path.class);
		paths.add(Paths.get("folder/relative"));
		try{fileService.getRelativeNames(paths, Paths.get("folder/relative"));fail();}
		catch(final IllegalArgumentException e){}
	}
	
	@Test
	public void testGetFilesRecursive() throws IOException {
		final Path base = testPath.resolve("testGetFilesRecursive");
		assertTrue(base.toFile().mkdir());
		Path testFile;
		Path testDir;
		
		// build a directory tree with some nested folders and files
		
		testFile = base.resolve("test1");
		assertTrue(testFile.toFile().createNewFile());
		testFile = base.resolve("test2.txt");
		assertTrue(testFile.toFile().createNewFile());
		testFile = base.resolve("test3.ignore");
		assertTrue(testFile.toFile().createNewFile());
		
		testDir = base.resolve("dir");
		assertTrue(testDir.toFile().mkdir());
		
		testFile = base.resolve("dir/test1");
		assertTrue(testFile.toFile().createNewFile());
		testFile = base.resolve("dir/test2.TXT");
		assertTrue(testFile.toFile().createNewFile());
		testFile = base.resolve("dir/test3.ignore");
		assertTrue(testFile.toFile().createNewFile());
		
		testDir = base.resolve("dir/dir1");
		assertTrue(testDir.toFile().mkdir());
		testDir = base.resolve("dir/dir2");
		assertTrue(testDir.toFile().mkdir());
		
		testFile = base.resolve("dir/dir1/test.txt");
		assertTrue(testFile.toFile().createNewFile());
		testFile = base.resolve("dir/dir2/test.txt");
		assertTrue(testFile.toFile().createNewFile());
		
		testDir = base.resolve("dir/dir2/dir");
		assertTrue(testDir.toFile().mkdir());
		
		// filter recursively and assert results (once per filter)
		{
			final PathFilter filter = path -> !path.getFileName().toString().endsWith(".ignore");
			final List<Path> result = fileService.getFilesRecursive(base, filter);
			
			final List<String> expected = new LinkedList<>(String.class);
			expected.add("dir/dir1/test.txt");
			expected.add("dir/dir2/test.txt");
			expected.add("dir/test1");
			expected.add("dir/test2.txt");
			expected.add("test1");
			expected.add("test2.txt");
			
			assertEquals(expected.size(), result.size());
			
			final Iterator<Path> resultIter = result.iterator();
			for (final String expectedString : expected) {
				assertEquals(
						base.resolve(expectedString).normalize(), resultIter.next().normalize());
			}
		}{
			final PathFilter filter = new FileService.FileExtensionFilter(new String[]{"txt"});
			final List<Path> result = fileService.getFilesRecursive(base, filter);
			
			final List<String> expected = new LinkedList<>(String.class);
			expected.add("dir/dir1/test.txt");
			expected.add("dir/dir2/test.txt");
			expected.add("dir/test2.TXT");
			expected.add("test2.txt");
			
			assertEquals(expected.size(), result.size());
			
			final Iterator<Path> resultIter = result.iterator();
			for (final String expectedString : expected) {
				assertEquals(
						base.resolve(expectedString).normalize(), resultIter.next().normalize());
			}
		}
		
		// clean up
		FileUtils.forceDelete(base.toFile());
	}
}
