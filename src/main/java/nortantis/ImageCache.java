package nortantis;

import nortantis.util.ImageHelper;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Caches icons in memory to avoid recreating or reloading them.
 */
public class ImageCache
{
	private static ImageCache instance;
	
	/**
	 * Maps original images, to scaled width, to scaled images.
	 */
	private final Map<BufferedImage, Map<Integer, BufferedImage>> scaledCache;
	
	/**
	 * Maps file path (or any string key) to images.
	 */
	private final Map<String, BufferedImage> fileCache;

	/**
	 * Maps string keys to images generated (not loaded directly from files).
	 */
	private final Map<String, BufferedImage> generatedImageCache;

	/**
	 * Singleton
	 */
	private ImageCache()
	{
		scaledCache = new ConcurrentHashMap<>();
		fileCache = new ConcurrentHashMap<>();
		generatedImageCache = new ConcurrentHashMap<>();
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
		return scaledCache.computeIfAbsent(icon, k -> ((Supplier<Map<Integer, BufferedImage>>) ConcurrentHashMap::new).get()).computeIfAbsent(width, k1 -> ((Supplier<BufferedImage>) () -> ImageHelper.scaleByWidth(icon, width)).get());
	}
	
	public BufferedImage getImageFromFile(Path path)
	{
		return fileCache.computeIfAbsent(path.toString(), k -> ((Supplier<BufferedImage>) () -> ImageHelper.read(path)).get());
	}
	
	public boolean containsImageFile(Path path)
	{
		return fileCache.containsKey(path.toString());
	}
	
	/**
	 * Get an image from cache or create it using createFun.
	 */
	public BufferedImage getOrCreateImage(String key, Supplier<BufferedImage> createFun)
	{
		return generatedImageCache.computeIfAbsent(key.toString(), k -> createFun.get());
	}
	
	public static void clear()
	{
		getInstance().scaledCache.clear();
		getInstance().fileCache.clear();
		getInstance().generatedImageCache.clear();
	}
}
