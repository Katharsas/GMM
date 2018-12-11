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
import java.util.function.Function;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

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
	
	Path testFolder;
	Path sourceFile;
	Path smallPreview;
	Path fullPreview;
	
	// total number of executions is samples * (warmups + repeats)
	private final int samplesPerMeasurement = 70;
	
	private final int warmups = 2;
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
	    testFolder = Paths.get("temp_testing");
		sourceFile = testFolder.resolve("test.tga").toAbsolutePath();
		smallPreview = testFolder.resolve("small.png").toAbsolutePath();
		fullPreview = testFolder.resolve("full.png").toAbsolutePath();
	}

	//@After
	public void tearDown() {
		try {
			Files.deleteIfExists(smallPreview);
			Files.deleteIfExists(fullPreview);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	@Test
	public void testRead() throws IOException {
		readAll(this::readImageFromFile);
	}
	
	private void readAll(Function<Path, BufferedImage> read) throws IOException {
		final Path folder = testFolder.resolve("testReadAll");
		final Path dest = folder.resolve("generated");
	  try (Stream<Path> tgas = Files.list(folder)) {
            tgas
                .filter(Files::isRegularFile)
                .findFirst()
                .ifPresent(tga -> {
                    final String name = tga.getFileName().toString();
                    System.out.println("Reading " + name);
//                    consume(read.apply(tga));
                    service.createPreviewsImgScalr(tga, dest.resolve("f_" + name + ".png"), dest.resolve("s_" + name + ".png"));
                });
        }
	}
	
	
	// just to be sure: public so compiler doesn't optimize away the image reads
	public int[] pixelConsumerInt;
	public byte[] pixelConsumerByte;
	
	@Test
	public void testReadSpeed() throws IOException {
		warmupReads(this::readImageFromFile);
		//warmupReads(this::readImageFromStream);
		//warmupReads(this::readImageFromStreamBuffered);
		
		measureReads("readImageFromFile", this::readImageFromFile);
		//measureReads("readImageFromStream", this::readImageFromStream);
		//measureReads("readImageFromStreamBuffered", this::readImageFromStreamBuffered);
	}
	
	private void warmupReads(Function<Path, BufferedImage> read) throws IOException {
		for (int i = 0; i < warmups * samplesPerMeasurement; i++) {
			consume(read.apply(sourceFile));
		}
	}
	
	private void measureReads(String desc, Function<Path, BufferedImage> read) throws IOException {
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
	
	private BufferedImage readImageFromFile(Path file) {
		try {
			return ImageIO.read(file.toFile());
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private BufferedImage readImageFromStream(Path file) {
		try {
			return ImageIO.read(new FileInputStream(file.toFile()));
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private BufferedImage readImageFromStreamBuffered(Path file) {
		try {
			return ImageIO.read(new BufferedInputStream(new FileInputStream(file.toFile())));
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	@Test
	public void testImgScalr() {
		System.out.println("\nImgScalr\n");
		for (int i = 0; i < 1; i++) {
			service.createPreviewsImgScalr(sourceFile, fullPreview, smallPreview);
		}
		for (int i = 0; i < repeats; i++) {
			final long start = System.currentTimeMillis();
			service.createPreviewsImgScalr(sourceFile, fullPreview, smallPreview);
			System.out.println(System.currentTimeMillis() - start);
		}
	}
}
