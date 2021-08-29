package nortantis;

import nortantis.util.ImageHelper;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static nortantis.util.ImageHelper.scaleByWidth;

/**
 * Caches icons in memory to avoid recreating or reloading them.
 */
public class ImageCache
{
	private static ImageCache instance;
	
	/**
	 * Maps original images, to scaled width, to scaled images.
	 */
	private final Map<BufferedImage, Map<Integer, BufferedImage>> scaledCache = new ConcurrentHashMap<>();
	
	/**
	 * Maps file path (or any string key) to images.
	 */
	private final Map<Path, BufferedImage> fileCache = new ConcurrentHashMap<>();

	/**
	 * Maps string keys to images generated (not loaded directly from files).
	 */
	private final Map<String, BufferedImage> generatedImageCache = new ConcurrentHashMap<>();

	/**
	 * Singleton
	 */
	private ImageCache()
	{
	}
	
	public synchronized static ImageCache getInstance()
	{
		if (instance == null)
			instance = new ImageCache();
		return instance;
	}
	
	public BufferedImage getScaledImage(BufferedImage icon, int width)
	{
		// There is a small chance the 2 different threads might both add the same image at the same time, 
		// but if that did happen it would only results in a little bit of duplicated work, not a functional
		// problem.
		return scaledCache
				.computeIfAbsent(icon, k -> new ConcurrentHashMap<>())
				.computeIfAbsent(width, k1 -> scaleByWidth(icon, width));
	}
	
	public BufferedImage getImageFromFile(Path path)
	{
		return fileCache
				.computeIfAbsent(path, k -> ImageHelper.read(path));
	}
	
	public boolean containsImageFile(Path path)
	{
		return fileCache.containsKey(path);
	}
	
	/**
	 * Get an image from cache or create it using createFun.
	 */
	public BufferedImage getOrCreateImage(String key, Supplier<BufferedImage> createFun)
	{
		return generatedImageCache.computeIfAbsent(key, k -> createFun.get());
	}
	
	public static void clear()
	{
		getInstance().scaledCache.clear();
		getInstance().fileCache.clear();
		getInstance().generatedImageCache.clear();
	}
}
