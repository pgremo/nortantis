package nortantis;

import hoten.geom.Point;
import hoten.voronoi.Center;
import hoten.voronoi.Corner;
import nortantis.editor.MapEdits;
import nortantis.util.*;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static java.awt.image.BufferedImage.TYPE_BYTE_BINARY;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Collections.max;
import static java.util.Collections.min;
import static java.util.Comparator.comparingDouble;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static nortantis.ListValuedMapCollector.toListValuedMap;

public class IconDrawer 
{
	public static final double mountainElevationThreshold = 0.58;
	public static final double hillElevationThreshold = 0.53;
	final double treeScale = 4.0/8.0;

	double meanPolygonWidth;
	// If a polygon is this number times meanPolygonWidth wide, no icon will be drawn on it.
	final double maxMeansToDraw = 5.0;
	double maxSizeToDrawIcon;
	private final int maxGapBetweenBiomeGroups = 2;
	private final MultiValuedMap<Center, IconDrawTask> iconsToDraw;
	WorldGraph graph;
	Random rand;
	/**
	 * Used to store icons drawn when generating icons so they can be edited later by the editor.
	 */
	public Map<Integer, CenterIcon> centerIcons;
	public Map<Integer, CenterTrees> trees;
	private final String cityIconsSetName;

	public IconDrawer(WorldGraph graph, Random rand, String cityIconsSetName)
	{
		iconsToDraw = new ArrayListValuedHashMap<>();
		this.graph = graph;
		this.rand = rand;
		this.cityIconsSetName = cityIconsSetName;
		
		meanPolygonWidth = findMeanPolygonWidth(graph);
		maxSizeToDrawIcon = meanPolygonWidth * maxMeansToDraw;
		centerIcons = new HashMap<>();
		trees = new HashMap<>();
	}

	public static double findMeanPolygonWidth(WorldGraph graph)
	{
		return graph.centers.stream()
				.mapToDouble(Center::findWidth)
				.filter(x -> x > 0.0)
				.average().orElse(0.0);
	}

	public void markMountains()
	{
		for (var c : graph.centers)
		{
			c.isMountain = c.elevation > mountainElevationThreshold
					&& !c.isCoast
					&& !c.isBorder && c.findWidth() < maxSizeToDrawIcon;
		}
	}

	public void markHills()
	{
		for (var c : graph.centers)
		{
			c.isHill = c.elevation < mountainElevationThreshold
					&& c.elevation > hillElevationThreshold
					&& !c.isCoast
					&& c.findWidth() < maxSizeToDrawIcon;
		}
	}
	
	public void markCities(double cityProbability)
	{
		for (var c : graph.centers)
		{
			if (c.isMountain || c.isHill || c.isWater) continue;
			double probability = rand.nextDouble();
			c.isCity = c.isRiver() && probability <= cityProbability * 2
					|| c.isCoast && probability <= cityProbability * 2
					|| probability <= cityProbability;
		}
	}
		
	/**
	 * Finds and marks mountain ranges, and groups smaller than ranges, and surrounding hills.
	 */
	public Tuple2<List<Set<Center>>, List<Set<Center>>> findMountainAndHillGroups()
	{
		// Max gap (in polygons) between mountains for considering them a single group. Warning:
		// there tend to be long polygons along edges, so if this value is much more than 2,
		// mountains near the ocean may be connected despite long distances between them..
		var maxGapSizeInMountainClusters = 2;
		var mountainGroups = findCenterGroups(graph, maxGapSizeInMountainClusters, center -> center.isMountain);

		var mountainAndHillGroups = findCenterGroups(graph, maxGapSizeInMountainClusters, center -> center.isMountain || center.isHill);

		// Assign mountain group ids to each center that is in a mountain group.
		var curId = 0;
		for (var group : mountainAndHillGroups)
		{
			for (var c : group)
			{
				c.mountainRangeId = curId;
			}
			curId++;
		}
		
		return new Tuple2<>(mountainGroups, mountainAndHillGroups);
		
	}
	
	/**
	 * This is used to add icon to draw tasks from map edits rather than using the generator to add them.
	 */
	public void clearAndAddIconsFromEdits(MapEdits edits, double sizeMultiplyer)
	{
		iconsToDraw.clear();
		var mountainImagesById = getAllIconGroupsAndMasksForType(IconType.mountains);
		var hillImagesById = getAllIconGroupsAndMasksForType(IconType.hills);
		var duneImages = getAllIconGroupsAndMasksForType(IconType.sand).get("dunes");
		var duneWidth = findDuneWidth();
		var cityImages = loadIconsWithWidths(IconType.cities);
		
		trees.clear();
		
		for (var cEdit : edits.centerEdits)
		{
			var center = graph.centers.get(cEdit.index);
			if (cEdit.icon != null)
			{
				if (cEdit.icon.iconType == CenterIconType.Mountain && cEdit.icon.iconGroupId != null && !mountainImagesById.isEmpty())
				{
					var groupId = cEdit.icon.iconGroupId;
					if (!mountainImagesById.containsKey(groupId))
					{
						// Someone removed the icon group. Choose a new group.
						groupId = chooseNewGroupId(mountainImagesById.keySet(), groupId);
					}
					if (mountainImagesById.get(groupId).size() > 0)
					{
						var scaledSize = findScaledMountainSize(center);
						var mountainImage = mountainImagesById.get(groupId).get(
								cEdit.icon.iconIndex % mountainImagesById.get(groupId).size()).first();
						var mask = mountainImagesById.get(groupId).get(
								cEdit.icon.iconIndex % mountainImagesById.get(groupId).size()).second();
						iconsToDraw.put(center, new IconDrawTask(mountainImage,
			       				mask, center.loc, scaledSize, true, false));
					}
				}
				else if (cEdit.icon.iconType == CenterIconType.Hill && cEdit.icon.iconGroupId != null && !hillImagesById.isEmpty())
				{
					var groupId = cEdit.icon.iconGroupId;
					if (!hillImagesById.containsKey(groupId))
					{
						// Someone removed the icon group. Choose a new group.
						groupId = chooseNewGroupId(hillImagesById.keySet(), groupId);
					}
					if (hillImagesById.get(groupId).size() > 0)
					{
						var scaledSize = findScaledHillSize(center);
						var hillImage = hillImagesById.get(groupId).get(
								cEdit.icon.iconIndex % hillImagesById.get(groupId).size()).first();
						var mask = hillImagesById.get(groupId).get(
								cEdit.icon.iconIndex % hillImagesById.get(groupId).size()).second();
						iconsToDraw.put(center, new IconDrawTask(hillImage,
			       				mask, center.loc, scaledSize, true, false));	
					}
				}
				else if (cEdit.icon.iconType == CenterIconType.Dune && duneWidth > 0 && duneImages != null && !duneImages.isEmpty())
				{
					var duneImage = duneImages.get(
							cEdit.icon.iconIndex % duneImages.size()).first();
					var mask = duneImages.get(
							cEdit.icon.iconIndex % duneImages.size()).second();
					iconsToDraw.put(center, new IconDrawTask(duneImage,
		       				mask, center.loc, duneWidth, true, false));								
				}
				else if (cEdit.icon.iconType == CenterIconType.City && !cityImages.isEmpty())
				{
					BufferedImage cityImage;
					BufferedImage mask;
					String cityIconName;
					if (cityImages.containsKey(cEdit.icon.iconName))
					{
						cityIconName = cEdit.icon.iconName;
					}
					else {
						cityImages.size();
						cityIconName = chooseNewGroupId(cityImages.keySet(), cEdit.icon.iconName);
					}
					if (cityIconName != null)
					{
						cityImage = cityImages.get(cityIconName).first();
						mask = cityImages.get(cityIconName).second();
						iconsToDraw.put(center,
								new IconDrawTask(cityImage, mask, center.loc, 
										(int)(cityImages.get(cityIconName).third() * sizeMultiplyer), true, true, cityIconName));
					}
				}

			}
			
			if (cEdit.trees != null)
			{
				trees.put(cEdit.index, cEdit.trees);
			}
		}

		drawTreesForAllCenters();
	}
	
	private String chooseNewGroupId(Set<String> groupIds, String oldGroupId)
	{
		var randomIndex = Math.abs(oldGroupId.hashCode() % groupIds.size());
		return groupIds.toArray(new String[0])[randomIndex];
	}

	/**
	 * Finds groups of centers that accepted according to a given function. A group is a set of centers
	 * for which there exists a path from any member of the set to any other such that you
	 * never have to skip over more than maxGapSize centers not accepted at once
	 * to get to that other center. If distanceThreshold > 1, the result will include those
	 * centers which connect centeres that are accepted.
	 */
	private static List<Set<Center>> findCenterGroups(WorldGraph graph, int maxGapSize,
			Function<Center, Boolean> accept)
	{
		var groups = new ArrayList<Set<Center>>();
		// Contains all explored centers in this graph. This prevents me from making a new group
		// for every center.
		var explored = new HashSet<Center>();
		for (var center : graph.centers)
		{
			if (accept.apply(center) && !explored.contains(center))
			{
				// Do a breadth-first-search from that center, creating a new group.
				// "frontier" maps centers to their distance from a center of the desired biome. 
				// 0 means it is of the desired biome.
				var frontier = new HashMap<Center, Integer>();
				frontier.put(center, 0);
				var group = new HashSet<Center>();
				group.add(center);
				while (!frontier.isEmpty())
				{
					var nextFrontier = new HashMap<Center, Integer>();
					for (var entry : frontier.entrySet())
					{
						for (var n : entry.getKey().neighbors)
						{
							if (!explored.contains(n))
							{
								if (accept.apply(n))
								{
									explored.add(n);
									group.add(n);
									nextFrontier.put(n, 0);
								}
								else if (entry.getValue() < maxGapSize)
								{
									nextFrontier.put(n, entry.getValue() + 1);
								}
							}
						}
					}
					frontier = nextFrontier;
				}
				groups.add(group);
				
			}
		}
		return groups;
	}


	/**
	 * 
=	 * @param mask A gray scale image which is white where the background should be drawn, and
	 * black where the map should be drawn instead of the background. This is necessary so that
	 * when I draw an icon that is transparent (such as a hand drawn mountain), I cannot see
	 * other mountains through it.
	 */
	private void drawIconWithBackgroundAndMask(BufferedImage map, BufferedImage icon, 
			BufferedImage mask, BufferedImage background, int xCenter, int yCenter, boolean ignoreMaxSize)
	{   	
    	if (map.getWidth() != background.getWidth())
    		throw new IllegalArgumentException();
       	if (map.getHeight() != background.getHeight())
    		throw new IllegalArgumentException();
       	if (mask.getWidth() != icon.getWidth())
       		throw new IllegalArgumentException("The given mask's width does not match the icon' width.");
       	if (mask.getHeight() != icon.getHeight())
       		throw new IllegalArgumentException("The given mask's height does not match the icon' height.");
       	
       	if (!ignoreMaxSize && icon.getWidth() > maxSizeToDrawIcon)
       		return;

		var xLeft = xCenter - icon.getWidth()/2;
		var yBottom = yCenter - icon.getHeight()/2;
      	
		var maskRaster = mask.getRaster();
		for (var x : new Range(icon.getWidth()))
			for (var y : new Range(icon.getHeight()))
			{
				var iconColor = new Color(icon.getRGB(x, y), true);
				var alpha = iconColor.getAlpha() / 255.0;
				// grey level of mask at the corresponding pixel in mask.
				var maskLevel = maskRaster.getSampleDouble(x, y, 0);
				Color bgColor;
				Color mapColor;
				// Find the location on the background and map where this pixel will be drawn.
				var xLoc = xLeft + x;
				var yLoc = yBottom + y;
				try
				{
					bgColor = new Color(background.getRGB(xLoc, yLoc));
					mapColor = new Color(map.getRGB(xLoc, yLoc));
				}
				catch (IndexOutOfBoundsException e)
				{
					// Skip this pixel.
					continue;
				}

				var red = (int)(alpha * (iconColor.getRed()) + (1 - alpha) * (maskLevel * bgColor.getRed() + (1 - maskLevel) * mapColor.getRed()));
				var green = (int)(alpha * (iconColor.getGreen()) + (1 - alpha) * (maskLevel * bgColor.getGreen() + (1 - maskLevel) * mapColor.getGreen()));
				var blue = (int)(alpha * (iconColor.getBlue()) + (1 - alpha) * (maskLevel * bgColor.getBlue() + (1 - maskLevel) * mapColor.getBlue()));
				
				map.setRGB(xLoc, yLoc, new Color(red, green, blue).getRGB());
			}
	}

	/**
	 * Draws all icons in iconsToDraw. I draw all the icons at once this way so that I can sort
	 * the icons by the y-coordinate of the base of each icon. This way icons lower on the map
	 * are drawn in front of those that are higher.
	 */
	public void drawAllIcons(BufferedImage map, BufferedImage background)
	{	
		iconsToDraw.entries().stream()
				.filter(not(entry -> entry.getKey().isWater))
				.map(Map.Entry::getValue)
				.map(IconDrawTask::scaleIcon)
				.filter(not(this::isIconTouchingWater))
				.forEach(task -> drawIconWithBackgroundAndMask(map, task.icon, task.mask, background, (int)task.centerLoc.x,
						(int)task.centerLoc.y, task.ignoreMaxSize));
	}
	
	/**
	 * Adds icon draw tasks to draw cities.
	 * Side effect if a city is placed where it cannot be drawn, this will un-mark it as a city.
	 * @return IconDrawTask of each city icon added. Needed to avoid drawing text on top of cities.
	 */
	public List<IconDrawTask> addOrUnmarkCities(double sizeMultiplyer, boolean addIconDrawTasks)
	{
		var cityIcons = loadIconsWithWidths(IconType.cities);
		if (cityIcons.isEmpty())
		{
			Logger.println("Cities will not be drawn because there are no city icons.");
			return new ArrayList<>(0);
		}
		
		var cityNames = new ArrayList<>(cityIcons.keySet());
		
		var cities = new ArrayList<IconDrawTask>();
		
		for (Center c : graph.centers)
		{
			if (c.isCity)
			{
				var cityName = cityNames.get(rand.nextInt(cityNames.size()));
				var scaledWidth = (int)(cityIcons.get(cityName).third() * sizeMultiplyer);
				var icon = cityIcons.get(cityName).first();

				var task = new IconDrawTask(icon, cityIcons.get(cityName).second(), c.loc, scaledWidth, true, true, cityName);
				if (!isIconTouchingWater(task))
				{
					if (addIconDrawTasks)
					{
						iconsToDraw.put(c, task);
		           		centerIcons.put(c.index, new CenterIcon(CenterIconType.City, cityName));
					}
	           		
	    	   		cities.add(task); 
				}
				else
				{
					c.isCity = false;
				}
			}
		}
		
		return cities;
	}

	public void addMountainsAndHills(List<Set<Center>> mountainAndHillGroups)
	{				
        // Maps mountain range ids (the ids in the file names) to list of mountain images and their masks.
        var mountainImagesById = getAllIconGroupsAndMasksForType(IconType.mountains);
        if (mountainImagesById.isEmpty())
        {
        	Logger.println("No mountain images were found. Mountain images will not be drawn.");
        	return;
        }

        // Maps mountain range ids (the ids in the file names) to list of hill images and their masks.
        // The hill image file names must use the same ids as the mountain ranges.
		var hillImagesById = getAllIconGroupsAndMasksForType(IconType.hills);
        
        // Warn if images are missing
        for (var hillGroupId : hillImagesById.keySet())
        {
        	if (!mountainImagesById.containsKey(hillGroupId))
        	{
        		Logger.println("No mountain images found for the hill group \"" + hillGroupId + "\". Those hill images will be ignored.");
        	}
        }
        for (var mountainGroupId : mountainImagesById.keySet())
        {
        	if (!hillImagesById.containsKey(mountainGroupId))
        	{
        		Logger.println("No hill images found for the mountain group \"" + mountainGroupId + "\". That mountain group will not have hills.");
        	}
        }

        // Maps from the mountainRangeId of Centers to the range id's from the mountain image file names.
        var rangeMap = new TreeMap<Integer, String>();
        
        for (var group : mountainAndHillGroups)
        {
        	for (var c : group)
        	{
				var fileNameRangeId = rangeMap.get(c.mountainRangeId);
	        	if ((fileNameRangeId == null))
	        	{
	        		fileNameRangeId =  new ArrayList<>(mountainImagesById.keySet()).get(
	        				rand.nextInt(mountainImagesById.keySet().size()));
	        		rangeMap.put(c.mountainRangeId, fileNameRangeId);
	        	}

	        	if (c.isMountain)
	        	{
					var imagesInRange = mountainImagesById.get(fileNameRangeId);


		        	// I'm deliberately putting this line before checking center size so that the
		        	// random number generator is used the same no matter what resolution the map
		        	// is drawn at.
					var i = rand.nextInt(imagesInRange.size());

					var scaledSize = findScaledMountainSize(c);

	           		// Make sure the image will be at least 1 pixel wide.
		           	if (scaledSize >= 1)
		           	{	
			           	// Draw the image such that it is centered in the center of c.
		           		iconsToDraw.put(c, new IconDrawTask(imagesInRange.get(i).first(),
		           				imagesInRange.get(i).second(), c.loc, scaledSize, true, false));
		           		centerIcons.put(c.index, new CenterIcon(CenterIconType.Mountain, fileNameRangeId, i));
		           	}
		        }
	         	else if (c.isHill)
	         	{
					var imagesInGroup =
		        			hillImagesById.get(fileNameRangeId);
		        	
		        	if (imagesInGroup != null && !imagesInGroup.isEmpty())
		        	{
						var i = rand.nextInt(imagesInGroup.size());

						var scaledSize = findScaledHillSize(c);
			        	
		           		// Make sure the image will be at least 1 pixel wide.
			           	if (scaledSize >= 1)
			           	{
			           		iconsToDraw.put(c, new IconDrawTask(imagesInGroup.get(i).first(),
			           				imagesInGroup.get(i).second(), c.loc, scaledSize, true, false));
			           		centerIcons.put(c.index, new CenterIcon(CenterIconType.Hill, fileNameRangeId, i));
			           	}
		        	}         		
	         	}
        	}
        }

	}
	
	private int findScaledMountainSize(Center c)
	{
		// Find the center's size along the x axis.
		var cSize = findCenterWidthBetweenNeighbors(c);
		// Mountain images are scaled by this.
		var mountainScale = 1.0;
		return (int)(cSize * mountainScale);
	}
	
	private int findScaledHillSize(Center c)
	{
		// Find the center's size along the x axis.
		var cSize = findCenterWidthBetweenNeighbors(c);
		// Hill images are scaled by this.
		var hillScale = 0.5;
		return (int)(cSize * hillScale);
	}
	
	public void addSandDunes()
	{
		var sandGroups = getAllIconGroupsAndMasksForType(IconType.sand);
		if (sandGroups.isEmpty())
		{
			Logger.println("Sand dunes will not be drawn because no sand images were found.");
			return;
		}
		
        // Load the sand dune images.
		var duneImages = sandGroups.get("dunes");
        
        if (duneImages == null || duneImages.isEmpty())
        {
			Logger.println("Sand dunes will not be drawn because no sand dune images were found.");
			return;
        }
        
   		var groups = findCenterGroups(graph, maxGapBetweenBiomeGroups,
				center -> center.biome.equals(Biome.TEMPERATE_DESERT));
   		
   		// This is the probability that a temperate desert will be a dune field.
		var duneProbabilityPerBiomeGroup = 0.6;
		var duneProbabilityPerCenter = 0.5;

		var width = findDuneWidth();
		if (width == 0)
			return;
		
   		
		for (var group : groups)
		{	
			if (rand.nextDouble() < duneProbabilityPerBiomeGroup)
			{
				for (var c : group)
				{	        
					if (rand.nextDouble() < duneProbabilityPerCenter)
					{
						c.isSandDunes = true;

						var i = rand.nextInt(duneImages.size());
		           		iconsToDraw.put(c, new IconDrawTask(duneImages.get(i).first(),
		           				duneImages.get(i).second(), c.loc, width, true, false));
		           		centerIcons.put(c.index, new CenterIcon(CenterIconType.Dune, "sand", i));
					}
				}
				
			}
		}
	}
	
	private int findDuneWidth()
	{
		return (int)(findMeanPolygonWidth(graph) * 1.5);
	}
		
	public void addTrees() {
		addCenterTrees();
		drawTreesForAllCenters();
	}
	
	public static Set<TreeType> getTreeTypesForBiome(Biome biome)
	{
		return Arrays.stream(ForestType.values())
				.filter(forest -> forest.biome == biome)
				.map(forest -> forest.treeType)
				.collect(Collectors.toCollection(TreeSet::new));
	}
	
	private void addCenterTrees()
	{	
		trees.clear();
        
        for (var forest : ForestType.values())
        {
        	if (forest.biomeFrequency != 1.0)
        	{
				var groups = findCenterGroups(graph, maxGapBetweenBiomeGroups,
						center -> center.biome.equals(forest.biome));
        		for (var group : groups)
        		{
        			if (rand.nextDouble() < forest.biomeFrequency)
        			{
        				for (var c : group)
        				{
               				if (canGenerateTreesOnCenter(c))
               				{
               				   trees.put(c.index, new CenterTrees(forest.treeType.toString().toLowerCase(), forest.density, c.treeSeed));
               				}
        				}
        			}
        		}
        	}
        }
 
        // Process forest types that don't use biome groups separately for efficiency.
        for (var c : graph.centers)
        {
    		for (var forest : ForestType.values())
    		{
    			if (forest.biomeFrequency == 1.0)
    			{
        			if (forest.biome.equals(c.biome))
        			{
        				if (canGenerateTreesOnCenter(c))
        				{
        					trees.put(c.index, new CenterTrees(forest.treeType.toString().toLowerCase(), forest.density, c.treeSeed));
        				}
        			}
    			}

 	        }
        }
	}
	
	private boolean canGenerateTreesOnCenter(Center c)
	{
		return c.elevation < mountainElevationThreshold && !c.isWater && !c.isCoast;
	}
	
	/**
	 * Draws all trees in this.trees.
	 */
	public void drawTreesForAllCenters()
	{
		// Find the average width of all Centers.
		var avgHeight = graph.centers.stream()
				.mapToDouble(this::findCenterWidthBetweenNeighbors)
				.average().orElse(0.0);

		// Make the tree images small. I make them all the same height.
		var scaledHeight = (int)(avgHeight * treeScale);
		if (scaledHeight == 0)
		{
			// Don't draw trees if they would all be size zero.
			return;
		}

		// Load the images and masks.
		var getKey = (Function<Map.Entry<String, Tuple2<BufferedImage, BufferedImage>>, String>) Map.Entry::getKey;
		var getValue = (Function<Map.Entry<String, Tuple2<BufferedImage, BufferedImage>>, Tuple2<BufferedImage, BufferedImage>>) x -> new Tuple2<>(ImageHelper.scaleByHeight(x.getValue().first(), scaledHeight), ImageHelper.scaleByHeight(x.getValue().second(), scaledHeight));
		var treesById = getAllIconGroupsAndMasksForType(IconType.trees).entries().stream()
				.collect(toListValuedMap(getKey, getValue));
        if (treesById.isEmpty())
        {
			Logger.println("Trees will not be drawn because no tree images were found.");
			return;
        }
        
         // Store which corners have had trees drawn so that I don't draw them multiple times.
		var cornersWithTreesDrawn = new boolean[graph.corners.size()];
        
        for (Center c : graph.centers)
        {
			var cTrees = trees.get(c.index);
        	if (cTrees != null)
        	{
        		if (cTrees.treeType != null && treesById.containsKey(cTrees.treeType))
        		{
		        	drawTreesAtCenterAndCorners(cTrees.density, treesById.get(cTrees.treeType), avgHeight,
								cornersWithTreesDrawn, c, cTrees.randomSeed);
				}
        	}
        }
	}

	private void drawTreesAtCenterAndCorners(double density, List<Tuple2<BufferedImage, BufferedImage>> imagesAndMasks, double avgCenterHeight,
											 boolean[] cornersWithTreesDrawn, Center center, long randomSeed)
	{
		var rand = new Random(randomSeed);
		drawTrees(imagesAndMasks, avgCenterHeight, center.loc, density, center, rand);
			
		// Draw trees at the neighboring corners too.
		for (Corner corner : center.corners)
		{
			if (!cornersWithTreesDrawn[corner.index])
			{
				drawTrees(imagesAndMasks, avgCenterHeight, corner.loc,
						density, center, rand);
				cornersWithTreesDrawn[corner.index] = true;
			}
		}
	}

	private enum ForestType{
		TEMPERATE_RAIN_FOREST(TreeType.Deciduous, Biome.TEMPERATE_RAIN_FOREST, 0.5, 1.0),
		TAIGA(TreeType.Pine, Biome.TAIGA, 1.0, 1.0),
		SHRUBLAND(TreeType.Pine, Biome.SHRUBLAND, 1.0, 1.0),
		HIGH_TEMPERATE_DECIDUOUS_FOREST(TreeType.Pine, Biome.HIGH_TEMPERATE_DECIDUOUS_FOREST, 1.0, 0.25),
		HIGH_TEMPERATE_DESERT(TreeType.Cacti, Biome.HIGH_TEMPERATE_DESERT, 1.0/8.0, 0.1);

		final TreeType treeType;
		final Biome biome;
		final double density;
		final double biomeFrequency;

		/**
		 * @param biomeFrequency If this is not 1.0, groups of centers of biome type "biome" will be found
		 * and each groups will have this type of forest with probability biomeProb.
		 */
		ForestType(TreeType treeType, Biome biome, double density, double biomeFrequency)
		{
			this.treeType = treeType;
			this.biome = biome;
			this.density = density;
			this.biomeFrequency = biomeFrequency;
		}
	}

	private double findCenterWidthBetweenNeighbors(Center c)
	{
		var eastMostNeighbor = max(c.neighbors, comparingDouble(c2 -> c2.loc.x));
		var westMostNeighbor = min(c.neighbors, comparingDouble(c2 -> c2.loc.x));
		return Math.abs(eastMostNeighbor.loc.x - westMostNeighbor.loc.x);
	}

	private void drawTrees(List<Tuple2<BufferedImage, BufferedImage>> imagesAndMasks, double cSize, Point loc,
						   double forestDensity, Center center, Random rand)
	{
		if (imagesAndMasks == null || imagesAndMasks.isEmpty())
		{
			return;
		}
		
		// Convert the forestDensity into an integer number of trees to draw such that the expected
		// value is forestDensity.
		var fraction = forestDensity - (int)forestDensity;
		var extra = rand.nextDouble() < fraction ? 1 : 0;
		var numTrees = ((int)forestDensity) + extra;
		       	
       	for (var i = 0; i < numTrees; i++)
       	{
			var item = imagesAndMasks.get(rand.nextInt(imagesAndMasks.size()));
			var image = item.first();
			var mask = item.second();
           	     
           	// Draw the image such that it is centered in the center of c.
			var x = (int) loc.x;
			var y = (int) loc.y;

			var sqrtSize = Math.sqrt(cSize);
           	x += rand.nextGaussian() * sqrtSize*2.0;
           	y += rand.nextGaussian() * sqrtSize*2.0;
        	
           	iconsToDraw.put(center, new IconDrawTask(image, mask, new Point(x, y), image.getWidth(), false, false));
       	}
	}
	
	private boolean isIconTouchingWater(IconDrawTask iconTask)
	{       	
       	int imageUpperLeftX = (int)iconTask.centerLoc.x - iconTask.scaledWidth/2;
       	int imageUpperLeftY = (int)iconTask.centerLoc.y + iconTask.scaledHeight/2;

       	// Only check precision*precision points.
       	var precision = DoubleStream.of(iconTask.scaledWidth, iconTask.scaledHeight, 32)
				.min().orElse(0.0);
       	for (int x = 0; x < precision; x++)
       	{
       		for (int y = 0; y < precision; y++)
       		{
       			Center center = graph.findClosestCenter(imageUpperLeftX + (int)(iconTask.scaledWidth * (x/precision)), 
       					(imageUpperLeftY - (int)(iconTask.scaledHeight * (y/precision))));
       	       	if (center.isWater)
       	       		return true;
       		}
       	}
       
       	return false;
	}
	
	/**
	 * Loads icons which do not have groups, but which do have default widths in the file names.
	 * 
	 * @return A map from icon names (not including width or extension) to a tuple with the icon, mask, and width.
	 * @param iconType
	 */
	private Map<String, Tuple3<BufferedImage, BufferedImage, Integer>> loadIconsWithWidths(final IconType iconType)
	{
		var imagesAndMasks = new HashMap<String, Tuple3<BufferedImage, BufferedImage, Integer>>();
		var fileNames = getIconGroupFileNames(iconType, null, getIconSetName(iconType));
		fileNames.forEach(fileName ->{
			var fileNameBaseWithoutWidth = getFileNameBaseWithoutWidth(fileName);
			if (imagesAndMasks.containsKey(fileNameBaseWithoutWidth))
			{
				throw new RuntimeException("There are multiple icons for %s named '%s' whose file names only differ by the width. Rename one of them".formatted(iconType, fileNameBaseWithoutWidth));
			}

			var path = getIconGroupPath(iconType, null, getIconSetName(iconType)).resolve(fileName);
			var imageCache = ImageCache.getInstance();
			if (!imageCache.containsImageFile(path))
			{
				Logger.println("Loading icon: %s".formatted(path));
			}

			var icon = imageCache.getImageFromFile(path);
			var mask = imageCache.getOrCreateImage(format("mask %s", path), () -> createMask(icon));

			var parts = FilenameUtils.getBaseName(fileName).split("width=");
			if (parts.length < 2)
			{
				throw new RuntimeException("The icon %s of type %s must have its default width stored at the end of the file name in the format width=<number>. Example: myCityIcon width=64.png.".formatted(fileName, iconType));
			}
			try
			{
				var widthStr = parts[parts.length - 1];
				var width = parseInt(widthStr);
				imagesAndMasks.put(fileNameBaseWithoutWidth, new Tuple3<>(icon, mask, width));
			}
			catch (RuntimeException e)
			{
				throw new RuntimeException("Unable to load icon %s. Make sure the default width of the image is stored at the end of the file name in the format width=<number>. Example: myCityIcon width=64.png. Error: %s".formatted(path, e.getMessage()), e);
			}
		});
		return imagesAndMasks;
	}

	private ListValuedMap<String, Tuple2<BufferedImage, BufferedImage>> getAllIconGroupsAndMasksForType(final IconType iconType)
	{
		var imagesPerGroup = new ArrayListValuedHashMap<String, Tuple2<BufferedImage, BufferedImage>>();

		getIconGroupNames(iconType).forEach(groupName -> {
			var fileNames = getIconGroupFileNames(iconType, groupName, getIconSetName(iconType));
			var groupPath = getIconGroupPath(iconType, groupName, getIconSetName(iconType));

			fileNames
					.map(groupPath::resolve)
					.map(x -> {
						var icon = ImageCache.getInstance().getImageFromFile(x);
						var mask = ImageCache.getInstance().getOrCreateImage(format("mask %s", x), () -> createMask(icon));

						return  new Tuple2<>(icon, mask);
					})
					.collect(toListValuedMap(x -> groupName, x -> x, () -> imagesPerGroup));
		});
		return imagesPerGroup;
	}
	
	private String getIconSetName(IconType iconType)
	{
		return iconType.equals(IconType.cities) ? cityIconsSetName : null;
	}
	
	public static Set<String> getIconSets(IconType iconType)
	{
		if (!doesUseSets(iconType))
		{
			throw new RuntimeException("Type '%s' does not use sets.".formatted(iconType));
		}

		var path = AssetsPath.get("icons", iconType.toString());
		try {
			return Files.list(path)
					.filter(Files::isDirectory)
					.map(Path::getFileName)
					.map(Path::toString)
					.collect(toSet());
		} catch (IOException e) {
			Logger.println(e.getMessage());
			return Set.of();
		}
	}
	
	public static Set<String> getIconGroupFileNamesWithoutWidthOrExtension(IconType iconType, String groupName, String cityIconSetName)
	{
		return getIconGroupFileNames(iconType, groupName, cityIconSetName)
				.map(IconDrawer::getFileNameBaseWithoutWidth)
				.collect(toSet());
	}
	
	private static String getFileNameBaseWithoutWidth(String fileName)
	{
		return fileName.substring(0, fileName.lastIndexOf("width="));
	}
	
	/**
	 * Gets the names of icon groups, which are folders under the icon type folder or the icon set folder which
	 * contain images files.
	 * @param iconType Name of a folder under assets/icons
	 * @param setName Optional - for icon types that support it, it is a folder under assets/icons/<iconType>/
	 * @return Array of file names sorted with no duplicates
	 */
	public static Stream<String> getIconGroupNames(IconType iconType, String setName)
	{
		Path path;
		if (doesUseSets(iconType))
		{
			if (setName == null || setName.isEmpty())
			{
				return Stream.empty();
			}
			path = AssetsPath.get( "icons", iconType.toString(), setName);
		}
		else
		{
			path = AssetsPath.get( "icons", iconType.toString());
		}

		try {
			return Files.list(path)
					.filter(Files::isDirectory)
					.map(Path::getFileName)
					.map(Path::toString)
					.sorted();
		} catch (IOException e) {
			Logger.println(e.getMessage());
			return Stream.empty();
		}
	}
	
	public Stream<String> getIconGroupNames(IconType iconType)
	{
		return getIconGroupNames(iconType, getIconSetName(iconType));
	}
	
	public static Stream<String> getIconGroupFileNames(IconType iconType, String groupName, String setName)
	{
		try {
			return Files.list(getIconGroupPath(iconType, groupName, setName))
					.filter(Files::isRegularFile)
					.map(Path::getFileName)
					.map(Path::toString)
					.sorted();
		} catch (IOException e) {
			Logger.println(e.getMessage());
			return Stream.empty();
		}
	}
	
	public static Path getIconGroupPath(IconType iconType, String groupName, String setName)
	{
		Path path;
		if (iconType.equals(IconType.cities))
		{
			path = AssetsPath.get("icons", iconType.toString(), setName);
		}
		else
		{
			if (doesUseSets(iconType))
			{
				// Not used yet
				
				if (setName == null || setName.isEmpty())
				{
					throw new IllegalArgumentException("The icon type %s uses sets, but no set name was given.".formatted(iconType));
				}
				path = AssetsPath.get("icons", iconType.toString(), groupName, setName);
			}
			else
			{
				path = AssetsPath.get( "icons", iconType.toString(), groupName);
			}
		}
		return path;
	}
	
	/**
	 * Tells whether an icon type supports icon sets. Icon sets are an additional folder under the icon type folder
	 * which is the name of the icon set. Under the icon set folder either image files or group folders of image files.
	 * 
	 * If an icon type supports sets, it should also return a value from getSetName.
	 * @param iconType query
	 * @return true if it uses the iconType set
	 */
	private static boolean doesUseSets(IconType iconType)
	{
		return iconType.equals(IconType.cities);
	}
	
	private static final int opaqueThreshold = 50;
	
	/**
	 * Generates a mask image for an icon. A mask is used when drawing an icon to determine which pixels that are transparent in the icon
	 * should draw the map background vs draw the icons already drawn behind that icon. If a pixel is transparent in the icon, and the
	 * corresponding pixel is white in the mask, then the map background is drawn for that pixel. But if the map pixel is black, then there
	 * is no special handling when drawing that pixel, so whatever was drawn in that place on the map before it will be visible. 
	 * @param icon icon to create a mask for
	 * @return icon with mask applied
	 */
	public static BufferedImage createMask(BufferedImage icon)
	{
		// Top
		var topSilhouette = new BufferedImage(icon.getWidth(), icon.getHeight(), TYPE_BYTE_BINARY);
		{
			var points = new ArrayList<Coordinate>();
			for (var x = 0; x < icon.getWidth(); x++)
			{
				var point = findUppermostOpaquePixel(icon, x);
				if (point != null)
				{
					if (points.isEmpty())
					{
						points.add(new Coordinate(x, icon.getHeight()));
					}
					points.add(point);
				}
			}
			
			if (points.size() < 3)
			{
				return new BufferedImage(icon.getWidth(), icon.getHeight(), TYPE_BYTE_BINARY);
			}
			points.add(new Coordinate(points.get(points.size() - 1).x(), icon.getHeight()));
			drawWhitePolygonFromPoints(topSilhouette, points);
		}
		
		// Left side
		var leftSilhouette = new BufferedImage(icon.getWidth(), icon.getHeight(), TYPE_BYTE_BINARY);
		{
			var points = new ArrayList<Coordinate>();
			for (int y = 0; y < icon.getHeight(); y++)
			{
				var point = findLeftmostOpaquePixel(icon, y);
				if (point != null)
				{
					if (points.isEmpty())
					{
						points.add(new Coordinate(icon.getWidth(), y));
					}
					points.add(point);
				}
			}
			points.add(new Coordinate(icon.getWidth(), points.get(points.size() - 1).y()));
			drawWhitePolygonFromPoints(leftSilhouette, points);
		}

		// Right side
		var rightSilhouette = new BufferedImage(icon.getWidth(), icon.getHeight(), TYPE_BYTE_BINARY);
		{
			var points = new ArrayList<Coordinate>();
			for (int y = 0; y < icon.getHeight(); y++)
			{
				var point = findRightmostOpaquePixel(icon, y);
				if (point != null)
				{
					if (points.isEmpty())
					{
						points.add(new Coordinate(0, y));
					}
					points.add(point);
				}
			}
			points.add(new Coordinate(0, points.get(points.size() - 1).y()));
			drawWhitePolygonFromPoints(rightSilhouette, points);
		}
					
		// The mask image is a resolve of the intersection of the 3 silhouettes.

		var mask = new BufferedImage(icon.getWidth(), icon.getHeight(), TYPE_BYTE_BINARY);
		var g = mask.createGraphics();
		g.setColor(Color.white);
		var maskRaster = mask.getRaster();
		var topRaster = topSilhouette.getRaster();
		var leftRaster = leftSilhouette.getRaster();
		var rightRaster = rightSilhouette.getRaster();
		for (var x = 0; x < mask.getWidth(); x++)
		{
			for (var y = 0; y < mask.getHeight(); y++)
			{
				if (topRaster.getSample(x, y, 0) > 0 && leftRaster.getSample(x, y, 0) > 0 && rightRaster.getSample(x, y, 0) > 0)
				{
					maskRaster.setSample(x, y, 0, 255);
				}
			}
		}
		
		return mask;
	}
	
	private static void drawWhitePolygonFromPoints(BufferedImage image, List<Coordinate> points)
	{
		var xPoints = new int[points.size()];
		var yPoints = new int[points.size()];
		for (var i : new Range(points.size()))
		{
			 Coordinate point = points.get(i);
			 xPoints[i] = point.x();
			 yPoints[i] = point.y();
		}

		var g = image.createGraphics();
		g.setColor(Color.white);
		g.fillPolygon(xPoints, yPoints, xPoints.length);
	}
	
	private static Coordinate findUppermostOpaquePixel(BufferedImage icon, int x)
	{
		for (var y = 0; y < icon.getHeight(); y++)
		{
			var alpha = ImageHelper.getAlphaLevel(icon, x, y);
			if (alpha >= opaqueThreshold)
			{
				return new Coordinate(x, y);
			}
		}
		
		return null;
	}
	
	private static Coordinate findLeftmostOpaquePixel(BufferedImage icon, int y)
	{
		for (var x = 0; x < icon.getWidth(); x++)
		{
			var alpha = ImageHelper.getAlphaLevel(icon, x, y);
			if (alpha >= opaqueThreshold)
			{
				return new Coordinate(x, y);
			}
		}
		
		return null;
	}

	private static Coordinate findRightmostOpaquePixel(BufferedImage icon, int y)
	{
		for (var x = icon.getWidth() - 1; x >= 0 ; x--)
		{
			var alpha = ImageHelper.getAlphaLevel(icon, x, y);
			if (alpha >= opaqueThreshold)
			{
				return new Coordinate(x, y);
			}
		}
		
		return null;
	}

}
