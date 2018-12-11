package gmm.service.tasks;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import org.apache.commons.io.IOUtils;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.User;
import gmm.domain.task.TaskType;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.TextureProperties;
import gmm.domain.task.asset.TextureTask;
import gmm.service.FileService;
import gmm.service.data.Config;
import gmm.service.data.DataAccess;

/**
 * Should maybe be joined with {@link TextureAssetService} after Mesh implementation.
 * 
 * @author Jan
 */
@Service
public class TextureTaskService extends AssetTaskService<TextureProperties> {
	
	private static final String[] extensions = new String[] {"tga"};
	
	@Autowired
	public TextureTaskService(DataAccess data, Config config, FileService fileService) {
		super(data, config, fileService);
	}

	// changing the scaling size requires manual deletion of all generated previews
	private static final int SMALL_SIZE = 256;
	
	@Override
	protected String[] getExtensions() {
		return extensions;
	}
	
	@PostConstruct
	protected void init() {
		//register TGA loader plugin
		final IIORegistry registry = IIORegistry.getDefaultInstance();
		registry.registerServiceProvider(new com.realityinteractive.imageio.tga.TGAImageReaderSpi());
	}
	
	/**
	 * Create Preview files from texture file (for originals: only if they don't exist yet).
	 * For more texture operations see {@link gmm.service.tasks.TextureAssetService}
	 */
	@Override
	public CompletableFuture<TextureProperties> recreatePreview(
			Path sourceFile, Path previewFolder, AssetGroupType type) {
		
		final Path fullPreview = getPreviewFilePath(previewFolder, type, false);
		final Path smallPreview = getPreviewFilePath(previewFolder, type, true);
		
		return CompletableFuture.supplyAsync(() -> {
			fileService.testReadFile(sourceFile);
			return createPreviewsImgScalr(sourceFile, fullPreview, smallPreview);
		}, threadPool);
	}
	
	protected TextureProperties createPreviewsImgScalr(Path sourceFile, Path fullPreview, Path smallPreview) {
		BufferedImage image = readImage(sourceFile);
		final TextureProperties assetProps = newPropertyInstance();
		assetProps.setDimensions(image.getHeight(), image.getWidth());
		
		// full preview
		writeImage(image, fullPreview);
		//small preview
		if (image.getHeight() > SMALL_SIZE || image.getWidth() > SMALL_SIZE) {
			image = Scalr.resize(image, Method.QUALITY, SMALL_SIZE);
		}
		writeImage(image, smallPreview);
		return assetProps;
	}
	
	protected TextureProperties createPreviewsJavaCv(Path sourceFile, Path fullPreview, Path smallPreview) {
		final BufferedImage image = readImage(sourceFile);
		final TextureProperties assetProps = newPropertyInstance();
		assetProps.setDimensions(image.getHeight(), image.getWidth());
		
		// full preview
		writeImage(image, fullPreview);
		//small preview
		if (image.getHeight() > SMALL_SIZE || image.getWidth() > SMALL_SIZE) {
			final int height = image.getHeight();
			final int width = image.getWidth();
			final boolean hasAlpha = image.getColorModel().hasAlpha();
			final int imgType = hasAlpha ? opencv_core.CV_8UC4 : opencv_core.CV_8UC3;
			
			final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
			final Mat imageMat = new Mat(height, width, imgType, new BytePointer(pixels));
			
			final Mat resizedImage = new Mat();
			final Dimension dim = calculateResize(width, height, SMALL_SIZE);
			final int interpolationMethod = opencv_imgproc.INTER_AREA;
			opencv_imgproc.resize(imageMat, resizedImage, new Size(dim.width, dim.height),
			        0, 0, interpolationMethod);
			
			opencv_imgcodecs.imwrite(smallPreview.toString(), resizedImage);
			imageMat.release();
			resizedImage.release();
		} else {
			writeImage(image, smallPreview);
		}
		return assetProps;
	}
	
	private Dimension calculateResize(int width, int height, int targetSize) {
		int targetWidth = targetSize;
		int targetHeight = targetSize;
		if (width != height) {
			final boolean isWide = width >= height;
			if (isWide) {
				targetHeight = Math.round(((float)targetWidth / (float)width) * height);
			} else {
				targetWidth = Math.round(((float)targetHeight / (float)height) * width);
			}
		}
		return new Dimension(targetWidth, targetHeight);
	}
	
	private BufferedImage readImage(Path file) {
		try {
			return ImageIO.read(file.toFile());
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not read image at '" + file + "'!", e);
		}
	}
	
	private void writeImage(BufferedImage image, Path targetFile) {
		fileService.createDirectory(targetFile.getParent());
		try {
			ImageIO.write(image, "png", targetFile.toFile());
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not write image to '" + targetFile + ".png'!", e);
		}
	}
	
	@Override
	public void deletePreview(Path previewFolder, AssetGroupType isOriginal) {
		if (previewFolder.toFile().exists()) {
			final Path targetFull = getPreviewFilePath(previewFolder, isOriginal, false);
			final Path targetSmall = getPreviewFilePath(previewFolder, isOriginal, true);
			if (targetFull.toFile().exists()) fileService.delete(targetFull);
			if (targetSmall.toFile().exists()) fileService.delete(targetSmall);
		}
	}
	
	@Override
	protected boolean hasPreview(Path previewFolder, AssetGroupType isOriginal) {
		final Path targetFull = getPreviewFilePath(previewFolder, isOriginal, false);
		final Path targetSmall = getPreviewFilePath(previewFolder, isOriginal, true);
		return targetFull.toFile().isFile() && targetSmall.toFile().isFile();
	}
	
	private Path getPreviewFilePath(Path previewFolder, AssetGroupType isOriginal, boolean isSmall) {
		final String isOriginalString = isOriginal.getPreviewFileName();
		return previewFolder.resolve(isOriginalString + (isSmall ? "_small.png" : "_full.png"));
	}

	@Override
	protected TextureProperties newPropertyInstance() {
		return new TextureProperties();
	}

	@Override
	public TaskType getTaskType() {
		return TaskType.TEXTURE;
	}

	@Override
	protected TextureTask newInstance(AssetName assetName, User user) {
		return new TextureTask(user, assetName);
	}
	
	@Override
	public Path getAssetTypeSubFolder() {
		return config.subNewTextures();
	}
	
	/**
	 * Returns a texture preview image (png) as byte array.
	 */
	public void writePreview(TextureTask task, boolean small, String version, OutputStream target) {
		final String imageName = version + (small ? "_small" : "_full") + ".png";		
		final Path path = config.assetPreviews()
				.resolve(task.getAssetName().getKey().toString())
				.resolve(imageName);
		try(FileInputStream fis = new FileInputStream(path.toFile())) {
			IOUtils.copy(fis, target);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					"Could not write preview file from '" + path.toString() + "' to stream!", e);
		}
	}
}
