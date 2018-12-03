/**
 * 
 */
package gmm.service.tasks;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import gmm.service.FileService;
import gmm.service.data.MockConfig;

/**
 * @author Jan Mothes
 */
public class TexturePreviewTest {

	static TextureTaskService service;
	
	Path sourceFile;
	Path smallPreview;
	
	// total number of executions is samples * (warmups + repeats)
	private final int samplesPerMeasurement = 100;
	
	private final int warmups = 1;
	private final int repeats = 2;
	
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
		sourceFile = Paths.get("temp_testing/test.tga").toAbsolutePath();
		smallPreview = Paths.get("temp_testing/small.png").toAbsolutePath();
	}

	@After
	public void tearDown() {
		try {
			Files.deleteIfExists(smallPreview);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	// just to be sure: public so compiler doesn't optimize away the image reads
	public int[] pixelConsumerInt;
	public byte[] pixelConsumerByte;
	
	@Test
	public void testReadSpeed() throws IOException {
		warmupReads(this::readImageFromFile);
		warmupReads(this::readImageFromStream);
		warmupReads(this::readImageFromStreamBuffered);
		
		measureReads("readImageFromFile", this::readImageFromFile);
		measureReads("readImageFromStream", this::readImageFromStream);
		measureReads("readImageFromStreamBuffered", this::readImageFromStreamBuffered);
	}
	
	private void warmupReads(IOFunction<Path, BufferedImage> read) throws IOException {
		for (int i = 0; i < warmups * samplesPerMeasurement; i++) {
			consume(read.apply(sourceFile));
		}
	}
	
	private void measureReads(String desc, IOFunction<Path, BufferedImage> read) throws IOException {
		System.out.println("\n" + desc + "\n");
		for (int i = 0; i < repeats; i++) {
			final long start = System.currentTimeMillis();
			for (int j = 0; j < samplesPerMeasurement; j++) {
				consume(read.apply(sourceFile));
			}
			final long end = System.currentTimeMillis();
			System.out.println((end - start) / (samplesPerMeasurement + 0.0));
		}
	}
	
	private void consume(BufferedImage image) {
		final DataBuffer buffer = image.getRaster().getDataBuffer();
		if (buffer instanceof DataBufferByte) {
			pixelConsumerByte = ((DataBufferByte) buffer).getData();
		}
		if (buffer instanceof DataBufferInt) {
			pixelConsumerInt = ((DataBufferInt) buffer).getData();
		}
	}
	
	@FunctionalInterface
	public interface IOFunction<T, R> {
	   R apply(T t) throws IOException;
	}
	
	private BufferedImage readImageFromFile(Path file) throws IOException {
		return ImageIO.read(file.toFile());
	}
	
	private BufferedImage readImageFromStream(Path file) throws IOException {
		return ImageIO.read(new FileInputStream(file.toFile()));
	}
	
	private BufferedImage readImageFromStreamBuffered(Path file) throws IOException {
		return ImageIO.read(new BufferedInputStream(new FileInputStream(file.toFile())));
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
