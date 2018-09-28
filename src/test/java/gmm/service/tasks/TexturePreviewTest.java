/**
 * 
 */
package gmm.service.tasks;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import gmm.service.FileService;
import gmm.service.data.MockConfig;

/**
 * @author Jan
 *
 */
public class TexturePreviewTest {

	static TextureTaskService service;
	
	Path sourceFile;
	Path smallPreview;
	
	private final int repeats = 3;
	
	@BeforeClass
	public static void setUpBeforeClass() {
		service = new TextureTaskService(null, new MockConfig(null), new FileService());
		service.init();
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Before
	public void setUp() {
		sourceFile = Paths.get("test/test.tga").toAbsolutePath();
		smallPreview = Paths.get("test/small.png").toAbsolutePath();
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testA() {
		System.out.println("\nImgScalr\n");
		for (int i = 0; i < 1; i++) {
			service.createPreviewsImgScalr(sourceFile, null, smallPreview);
		}
		for (int i = 0; i < repeats; i++) {
			final long start = System.currentTimeMillis();
			service.createPreviewsImgScalr(sourceFile, null, smallPreview);
			System.out.println(System.currentTimeMillis() - start);
		}
	}
	@Test
	public void testB() {
		System.out.println("\nJavaCV\n");
		for (int i = 0; i < 1; i++) {
			service.createPreviewsJavaCv(sourceFile, null, smallPreview);
		}
		for (int i = 0; i < repeats; i++) {
			final long start = System.currentTimeMillis();
			service.createPreviewsJavaCv(sourceFile, null, smallPreview);
			System.out.println(System.currentTimeMillis() - start);
		}
	}
}
