package nortantis;

import hoten.geom.Point;
import hoten.voronoi.Center;
import hoten.voronoi.Corner;
import hoten.voronoi.Edge;
import nortantis.util.*;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.*;
import static java.util.Comparator.comparingDouble;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class TextDrawer
{
	private MapSettings settings;
	// How big a river must be , in terms of edge.river, to be considered for labeling.
	private final int riverMinWidth = 3;

	private BufferedImage landAndOceanBackground;
	private CopyOnWriteArrayList<MapText> mapTexts;
	private List<Area> cityAreas;
	Random r;
	long originalSeed;
	private final NameGenerator placeNameGenerator;
	private final NameGenerator personNameGenerator;
	private final NameCompiler nameCompiler;
	Area graphBounds;
	private final Font titleFontScaled;
	private final Font regionFontScaled;
	private final Font mountainRangeFontScaled;
	private final Font citiesAndOtherMountainsFontScaled;
	private final Font riverFontScaled;
	Set<String> namesGenerated;
	
	/**
	 * 
	 * @param settings The map settings to use. Some of these settings are for text drawing.
	 * @param sizeMultiplier The font size of text drawn will be multiplied by this value.
	 *  This allows the map to be scaled larger or smaller.
	 */
	public TextDrawer(MapSettings settings, double sizeMultiplier)
	{
		this.settings = settings;
		mapTexts = new CopyOnWriteArrayList<>();
		// I create a new Random instead of passing one in so that small differences in the way 
		// the random number generator is used previous to the TextDrawer do not change the text.
		this.r = new Random(settings.textRandomSeed);
		this.originalSeed = settings.textRandomSeed;
		this.namesGenerated = new HashSet<>();
				
		var placeNames = new ArrayList<String>();
		var personNames = new ArrayList<String>();
		var nounAdjectivePairs = new ArrayList<Pair<String>>();
		var nounVerbPairs = new ArrayList<Pair<String>>();
		for (String book : settings.books)
		{
			placeNames.addAll(readNameList(AssetsPath.get("books", book +"_place_names.txt")));
			personNames.addAll(readNameList(AssetsPath.get("books", book +"_person_names.txt")));
			nounAdjectivePairs.addAll(readStringPairs(AssetsPath.get("books", book + "_noun_adjective_pairs.txt")));
			nounVerbPairs.addAll(readStringPairs(AssetsPath.get("books", book + "_noun_verb_pairs.txt")));
		}

		double maxWordLengthComparedToAverage = 2.0;
		double probabilityOfKeepingNameLength1 = 0.0;
		double probabilityOfKeepingNameLength2 = 0.0;
		double probabilityOfKeepingNameLength3 = 0.3;
		placeNameGenerator = new NameGenerator(r, placeNames, maxWordLengthComparedToAverage, probabilityOfKeepingNameLength1, probabilityOfKeepingNameLength2, probabilityOfKeepingNameLength3);
		personNameGenerator = new NameGenerator(r, personNames, maxWordLengthComparedToAverage, probabilityOfKeepingNameLength1, probabilityOfKeepingNameLength2, probabilityOfKeepingNameLength3);
	
		nameCompiler = new NameCompiler(r, nounAdjectivePairs, nounVerbPairs);
		
		titleFontScaled = settings.titleFont.deriveFont(settings.titleFont.getStyle(),
				(int)(settings.titleFont.getSize() * sizeMultiplier));
		regionFontScaled = settings.regionFont.deriveFont(settings.regionFont.getStyle(),
				(int)(settings.regionFont.getSize() * sizeMultiplier));
		mountainRangeFontScaled = settings.mountainRangeFont.deriveFont(settings.mountainRangeFont.getStyle(),
				(int)(settings.mountainRangeFont.getSize() * sizeMultiplier));
		citiesAndOtherMountainsFontScaled = settings.otherMountainsFont.deriveFont(settings.otherMountainsFont.getStyle(),
				(int)(settings.otherMountainsFont.getSize() * sizeMultiplier));
		riverFontScaled = settings.riverFont.deriveFont(settings.riverFont.getStyle(),
				(int)(settings.riverFont.getSize() * sizeMultiplier));

	}
	
	private List<Pair<String>> readStringPairs(Path filename)
	{
		try {
			var counter = new AtomicInteger();
			return Files.readAllLines(filename)
					.stream()
					.peek(x -> counter.getAndIncrement())
					.filter(not(String::isBlank))
					.map(x -> x.split("\t"))
					.peek(x -> {
						if (x.length != 2)
							System.out.printf("Warning: No string pair found in %s at line %d.%n", filename, counter.get());
					})
					.filter(x -> x.length == 2)
					.map(x -> new Pair<>(x[0], x[1]))
					.collect(toList());
		} catch (IOException e) {
			throw new RuntimeException(format("Unable to read names from the file %s", filename), e);
		}
	}
	
	private List<String> readNameList(Path filename)
	{
		try {
			return Files.readAllLines(filename)
					.stream()
					.filter(not(String::isBlank))
					.collect(toList());
		} catch (IOException e) {
			throw new RuntimeException("Unable to read names from the file " + filename, e);
		}
	}
	
	public void drawText(WorldGraph graph, BufferedImage map, BufferedImage landAndOceanBackground,
			List<Set<Center>> mountainRanges, List<IconDrawTask> cityDrawTasks)
	{				
		this.landAndOceanBackground = landAndOceanBackground;
		cityAreas = cityDrawTasks.stream().map(IconDrawTask::createArea).collect(toList());

		if (settings.edits.text.size() > 0)
		{
			// Text has already been generated and the user has opened the panel to modify it.
			drawUserModifiedText(map, graph);
		}
		else
		{
			if (mountainRanges == null)
			{
				throw new IllegalStateException("mountainRanges must be given when generating text.");
			}
			generateText(map, graph, mountainRanges, cityDrawTasks);
		}
		
	}
	
	private void generateText(BufferedImage map, WorldGraph graph, List<Set<Center>> mountainRanges, List<IconDrawTask> cityDrawTasks)
	{
		// All text drawn must be done so in order from highest to lowest priority because if I try to draw
		// text on top of other text, the latter will not be displayed.
		
		graphBounds = new Area(new java.awt.Rectangle(0, 0, graph.getWidth(), graph.getHeight()));

		Graphics2D g = map.createGraphics();
		g.setColor(settings.textColor);
		
		addTitle(map, graph, g);
		
		g.setFont(citiesAndOtherMountainsFontScaled);
		// Get the height of the city/mountain font.
		FontMetrics metrics = g.getFontMetrics(g.getFont());
		int cityMountainFontHeight = getFontHeight(metrics);
		for (IconDrawTask city : cityDrawTasks)
		{
			Set<Point> cityLoc = new HashSet<>(1);
			cityLoc.add(city.centerLoc);
			String cityName = generateNameOfType(TextType.City, findCityTypeFromCityFileName(city.fileName), true);
			double cityYNameOffset = 18;
			drawNameRotated(map, g, cityName, cityLoc, city.scaledHeight/2.0 + (cityYNameOffset + cityMountainFontHeight/2.0) * settings.resolution, true, TextType.City);
		}
		
		g.setFont(regionFontScaled);
		for (Region region : graph.regions)
		{
			Set<Point> locations = extractLocationsFromCenters(region.getCenters());
			String name;
			try 
			{
				name = generateNameOfType(TextType.Region, null, true);
			}
			catch (NotEnoughNamesException ex)
			{
				throw new RuntimeException(ex.getMessage());
			}
			drawNameHorizontal(map, g, name, locations, graph, settings.drawBoldBackground,
					TextType.Region);
		}
				
		for (Set<Center> mountainRange : mountainRanges)
		{
			int mountainRangeMinSize = 50;
			if (mountainRange.size() >= mountainRangeMinSize)
			{
				g.setFont(mountainRangeFontScaled);
				Set<Point> locations = extractLocationsFromCenters(mountainRange);
				drawNameRotated(map, g, generateNameOfType(TextType.Mountain_range, null, true), locations, true, TextType.Mountain_range);
			}
			else
			{
				g.setFont(citiesAndOtherMountainsFontScaled);
				// y offset added to name of mountain groups smaller than a range.
				double mountainGroupYOffset = 67;
				if (mountainRange.size() >= 2)
				{
					if (mountainRange.size() == 2)
					{
						Point location = findCentroid(extractLocationsFromCenters(mountainRange));
						MapText text = createMapText(generateNameOfType(TextType.Other_mountains, OtherMountainsType.TwinPeaks, true), location, 0.0, TextType.Other_mountains);
						if (drawNameRotated(map, g, mountainGroupYOffset * settings.resolution, true, text))
						{
							mapTexts.add(text);
						}
					}
					else
					{
						drawNameRotated(map, g, generateNameOfType(TextType.Other_mountains, OtherMountainsType.Mountains, true), 
								extractLocationsFromCenters(mountainRange),
								mountainGroupYOffset * settings.resolution, true, TextType.Other_mountains);
					}
				}
				else
				{
					Point location = findCentroid(extractLocationsFromCenters(mountainRange));
					MapText text = createMapText(generateNameOfType(TextType.Other_mountains, OtherMountainsType.Peak, true), location, 0.0, TextType.Other_mountains);
					if (drawNameRotated(map, g, mountainGroupYOffset * settings.resolution, true, text))
					{
						mapTexts.add(text);
					}
				}
			}
		}
		
		g.setFont(riverFontScaled);
		List<River> rivers = findRivers(graph);
		for (River river : rivers)
		{
			// Rivers shorter than this will not be named. This must be at least 3.
			int riverMinLength = 3;
			if (river.size() >= riverMinLength)
			{
				int largeRiverWidth = 4;
				RiverType riverType = river.getWidth() >= largeRiverWidth ? RiverType.Large : RiverType.Small;
				
				Set<Point> locations = extractLocationsFromCorners(river.getCorners());
				// This is how far away from a river it's name will be drawn.
				double riverNameRiseHeight = -32;
				drawNameRotated(map, g, generateNameOfType(TextType.River, riverType, true), locations,
						riverNameRiseHeight * settings.resolution, true, TextType.River);										
			}
			
		}
		
		g.dispose();
	}
	
	private CityType findCityTypeFromCityFileName(String cityFileNameNoExtension)
	{
		String name = cityFileNameNoExtension.toLowerCase();
		if (Stream.of("fort", "castle", "keep", "citadel").anyMatch(name::contains))
		{
			return CityType.Fortification;
		}
		else if (name.contains("city") || name.contains("buildings"))
		{
			return CityType.City;
		}
		else if (Stream.of("town", "village", "houses").anyMatch(name::contains))
		{
			return CityType.Town;
		}
		else if (Stream.of("farm", "homestead", "building", "house").anyMatch(name::contains))
		{
			return CityType.Homestead;
		}
		else
		{
			return ProbabilityHelper.sampleEnumUniform(r, CityType.class);
		}
	}
	
	/**
	 * Draw text which was added by the user.
	 * @param map world image
	 * @param graph world graph
	 */
	public synchronized void drawUserModifiedText(BufferedImage map, WorldGraph graph)
	{
		Graphics2D g = map.createGraphics();

		g.setColor(settings.textColor);

		// Draw all text the user has (potentially) modified.
		for (MapText text : settings.edits.text)
		{
			if (text.value == null || text.value.trim().length() == 0)
			{
				continue;
			}
			
			Point textLocation = new Point(text.location.x * settings.resolution, text.location.y * settings.resolution);

			switch (text.type) {
				case Title:
					g.setFont(titleFontScaled);
					TectonicPlate plate = graph.getTectonicPlateAt(textLocation.x, textLocation.y);
					drawNameHorizontal(map, g, extractLocationsFromCenters(plate.centers),
							graph, settings.drawBoldBackground, false, text);
					break;
				case City:
				case Other_mountains:
					g.setFont(citiesAndOtherMountainsFontScaled);
					drawNameRotated(map, g, 0, false, text);
					break;
				case Region:
					g.setFont(regionFontScaled);
					Center center = graph.findClosestCenter(textLocation.x, textLocation.y);
					Set<Center> plateCenters;
					if (center.isWater) {
						plateCenters = findPlateCentersWaterOnly(center.tectonicPlate);
					} else {
						plateCenters = center.region.getCenters();
					}
					Set<Point> locations = extractLocationsFromCenters(plateCenters);
					drawNameHorizontal(map, g, locations, graph, settings.drawBoldBackground, false, text);
					break;
				case Mountain_range:
					g.setFont(mountainRangeFontScaled);
					drawNameRotated(map, g, 0, false, text);
					break;
				case River:
					g.setFont(riverFontScaled);
					drawNameRotated(map, g, 0, false, text);
					break;
			}
		}

		g.dispose();
	}
	
	/**
	 * Generates a name of the specified type. This is for when the user adds new text to the map.
	 * It is not used when the map text is first generated.

	 */
	public String generateNameOfTypeForTextEditor(TextType type)
	{
		if (List.of(TextType.Mountain_range, TextType.Other_mountains, TextType.River).contains(type))
		{
			nameCompiler.setSeed(System.currentTimeMillis());
		}
		
		Object subType = null;

		switch (type) {
			case Title:
				subType = ProbabilityHelper.sampleCategorical(r,
						ProbabilityHelper.createUniformDistributionOverEnumValues(TitleType.values()));
				break;
			case Other_mountains:
				subType = ProbabilityHelper.sampleCategorical(r,
						ProbabilityHelper.createUniformDistributionOverEnumValues(OtherMountainsType.values()));
				break;
			case River:
				subType = ProbabilityHelper.sampleCategorical(r,
						ProbabilityHelper.createUniformDistributionOverEnumValues(RiverType.values()));
				break;
			case City:
				// In the editor you add city icons in a different tool than city text, so we have no way of knowing what icon, if any, this city name is for.
				subType = ProbabilityHelper.sampleEnumUniform(r, CityType.class);
				break;
		}
		
		try
		{
			return generateNameOfType(type, subType, false);
		}
		catch (Exception e)
		{
			// This can happen if the selected books don't have enough names.
			return "name";
		}
	}
	
	/**
	 * Generate a name of a specified type.
	 * @param type The type of name
	 * @param subType A sub-type specific to the type specified. null means default type.
	 * @param requireUnique Whether generated names must be never seen in the extracted book names nor previously generated. 
	 * 		  If unique name generating fails, an exception will be thrown.
	 */
	private String generateNameOfType(TextType type, Object subType, boolean requireUnique)
	{
		switch (type) {
			case Title: {
				TitleType titleType = subType == null ? TitleType.Decorated : (TitleType) subType;

				double probabilityOfPersonName = 0.3;
				switch (titleType) {
					case Decorated:
						if (r.nextDouble() < probabilityOfPersonName) {
							return generatePersonName("The Land of %s", requireUnique);
						} else {
							return generatePlaceName("The Land of %s", requireUnique);
						}
					case NameOnly:
						return generatePlaceName("%s", requireUnique);
					default:
						throw new IllegalArgumentException("Unknown title type: " + titleType);
				}
			}
			case Region: {
				double probabilityOfPersonName = 0.2;
				if (r.nextDouble() < probabilityOfPersonName) {
					String format = ProbabilityHelper.sampleCategorical(r, Arrays.asList(
							new Tuple2<>(0.2, "Kingdom of %s"),
							new Tuple2<>(0.04, "Empire of %s")));
					return generatePersonName(format, requireUnique);
				} else {
					String format = ProbabilityHelper.sampleCategorical(r, Arrays.asList(
							new Tuple2<>(0.1, "Kingdom of %s"),
							new Tuple2<>(0.02, "Empire of %s"),
							new Tuple2<>(0.85, "%s")));
					return generatePlaceName(format, requireUnique);
				}
			}
			case Mountain_range: {
				double probabilityOfCompiledName = 0.7;
				if (r.nextDouble() < probabilityOfCompiledName) {
					return compileName("%s Range", requireUnique);
				} else {
					return generatePlaceName("%s Range", requireUnique);
				}
			}
			case Other_mountains: {
				OtherMountainsType mountainType = subType == null ? OtherMountainsType.Mountains : (OtherMountainsType) subType;
				String format = getOtherMountainNameFormat(mountainType);
				double probabilityOfCompiledName = 0.5;
				if (r.nextDouble() < probabilityOfCompiledName) {
					return compileName(format, requireUnique);
				} else {
					double probabilityOfPersonName = 0.4;
					if (r.nextDouble() < probabilityOfPersonName) {
						// Person name
						// Make the name possessive.
						format = format.replace("%s", "%s's");
						return generatePersonName(format, requireUnique);
					} else {
						return generatePlaceName(format, requireUnique);
					}
				}
			}
			case City: {
				CityType cityType = (CityType) subType;
				String structureName;
				switch (cityType) {
					case Fortification:
						structureName = ProbabilityHelper.sampleCategorical(r, Arrays.asList(
								new Tuple2<>(0.2, "Castle"),
								new Tuple2<>(0.2, "Fort"),
								new Tuple2<>(0.2, "Fortress"),
								new Tuple2<>(0.2, "Keep"),
								new Tuple2<>(0.2, "Citadel")));
						break;
					case City:
						structureName = ProbabilityHelper.sampleCategorical(r, Arrays.asList(
								new Tuple2<>(0.75, "City"),
								new Tuple2<>(0.25, "Town")));
						break;
					case Town:
						structureName = ProbabilityHelper.sampleCategorical(r, Arrays.asList(
								new Tuple2<>(0.2, "City"),
								new Tuple2<>(0.4, "Village"),
								new Tuple2<>(0.4, "Town")));
						break;
					case Homestead:
						structureName = ProbabilityHelper.sampleCategorical(r, Arrays.asList(
								new Tuple2<>(0.5, "Farm"),
								new Tuple2<>(0.5, "Village")));
						break;
					default:
						throw new RuntimeException("Unknown city type: " + cityType);
				}

				double probabilityOfPersonName = 0.5;
				if (r.nextDouble() < probabilityOfPersonName) {
					String format = ProbabilityHelper.sampleCategorical(r, Arrays.asList(
							new Tuple2<>(0.1, structureName + " of %s"),
							new Tuple2<>(0.04, "%s's " + structureName),
							new Tuple2<>(0.04, structureName + " of %s"),
							new Tuple2<>(0.04, structureName + " of %s"),
							new Tuple2<>(0.04, "%s's " + structureName),
							new Tuple2<>(0.04, "%s's " + structureName)));
					return generatePersonName(format, requireUnique);
				} else {
					String format = ProbabilityHelper.sampleCategorical(r, Arrays.asList(
							new Tuple2<>(0.2, structureName + " of %s"),
							new Tuple2<>(0.2, "%s " + structureName),
							new Tuple2<>(0.02, "%s " + structureName),
							new Tuple2<>(0.3, "%s")));
					return generatePlaceName(format, requireUnique);
				}
			}
			case River: {
				RiverType riverType = subType == null ? RiverType.Large : (RiverType) subType;
				String format = getRiverNameFormat(riverType);
				double probabilityOfCompiledName = 0.5;
				if (r.nextDouble() < probabilityOfCompiledName) {
					return compileName(format, requireUnique);
				}
				double probabilityOfPersonName = 0.4;
				if (r.nextDouble() < probabilityOfPersonName) {
					// Person name
					// Make the name possessive.
					format = format.replace("%s", "%s's");
					return generatePersonName(format, requireUnique);
				} else {
					return generatePlaceName(format, requireUnique);
				}
			}
			default:
				throw new UnsupportedOperationException("Unknown text type: " + type);
		}
	}
	
	private String getOtherMountainNameFormat(OtherMountainsType mountainType)
	{
		switch (mountainType)
		{
			case Mountains:
				return "%s Mountains";
			case Peak:
				return "%s Peak";
			case TwinPeaks:
				return "%s Twin Peaks";
			default:
				throw new RuntimeException("Unknown mountain group type: " + mountainType);
		}

	}
	
	private String getRiverNameFormat(RiverType riverType)
	{
		switch (riverType)
		{
			case Large:
				return ProbabilityHelper.sampleCategorical(r, Arrays.asList(
						new Tuple2<>(0.1, "%s Wash"),
						new Tuple2<>(0.8, "%s River")));
			case Small:
				return ProbabilityHelper.sampleCategorical(r, Arrays.asList(
						new Tuple2<>(0.1, "%s Bayou"),
						new Tuple2<>(0.2, "%s Creek"),
						new Tuple2<>(0.2, "%s Brook"),
						new Tuple2<>(0.5, "%s Stream")));
			default:
				throw new RuntimeException("Unknown river type: " + riverType);
		}
	}
	
	private enum OtherMountainsType
	{
		TwinPeaks,
		Mountains,
		Peak
	}
	
	private enum RiverType
	{
		Small,
		Large
	}
	
	private enum TitleType
	{
		NameOnly,
		Decorated
	}
	
	private enum CityType
	{
		Fortification,
		City,
		Town,
		Homestead,
	}
		
	public String generatePlaceName(String format, boolean requireUnique)
	{
		return innerCreateUniqueName(format, requireUnique, placeNameGenerator::generateName);
	}
	
	public String generatePersonName(String format, boolean requireUnique)
	{
		return innerCreateUniqueName(format, requireUnique, personNameGenerator::generateName);
	}

	
	private String compileName(String format, boolean requireUnique)
	{
		return innerCreateUniqueName(format, requireUnique, nameCompiler::compileName);
	}
	
	private String innerCreateUniqueName(String format, boolean requireUnique, Supplier<String> nameCreator)
	{
		final int maxRetries = 20;
		
		if (!requireUnique)
		{
			return 	format(format, nameCreator.get());
		}
		
		for (@SuppressWarnings("unused") int retry : new Range(maxRetries))
		{
			String name = format(format, nameCreator.get());
			if (!namesGenerated.contains(name))
			{
				namesGenerated.add(name);
				return name;
			}
		}
		throw new RuntimeException("Unable to create enough unique names. You can select more books, or shrink the world size, or try a different seed.");
		
	}
	
	private void addTitle(BufferedImage map, WorldGraph graph, Graphics2D g)
	{
		g.setFont(titleFontScaled);

		var oceanPlatesAndWidths = graph.plates.stream()
				.filter(p -> p.type == PlateType.Oceanic)
				.map(p -> new Tuple2<>(p, findWidth(p.centers)))
				.collect(toList());

		var landPlatesAndWidths = graph.plates.stream()
				.filter(p -> p.type == PlateType.Continental)
				.map(p -> new Tuple2<>(p, findWidth(p.centers)))
				.collect(toList());

		List<Tuple2<TectonicPlate, Double>> titlePlatesAndWidths;
		double thresholdForPuttingTitleOnLand = 0.3;
		if (landPlatesAndWidths.size() > 0 && ((double)oceanPlatesAndWidths.size()) / landPlatesAndWidths.size() < thresholdForPuttingTitleOnLand)
		{
			titlePlatesAndWidths = landPlatesAndWidths;
		}
		else
		{
			titlePlatesAndWidths = oceanPlatesAndWidths;
		}
				
		// Try drawing the title in each plate in titlePlatesAndWidths, starting from the widest plate to the narrowest.
		titlePlatesAndWidths.sort((t1, t2) -> -t1.getSecond().compareTo(t2.getSecond()));
		for (Tuple2<TectonicPlate, Double> plateAndWidth : titlePlatesAndWidths)
		{
			try
			{	
				if (drawNameHorizontal(map, g, generateNameOfType(TextType.Title, TitleType.Decorated, true),
						extractLocationsFromCenters(plateAndWidth.getFirst().centers), graph, settings.drawBoldBackground,
						TextType.Title))
				{
					return;
				}
				
				// The title didn't fit. Try drawing it with just a name.
				if (drawNameHorizontal(map, g, generateNameOfType(TextType.Title, TitleType.NameOnly, true),
						extractLocationsFromCenters(plateAndWidth.getFirst().centers), graph, settings.drawBoldBackground,
						TextType.Title))
				{
					return;
				}
			}
			catch (NotEnoughNamesException e)
			{
				throw new RuntimeException(e.getMessage());
			}
		}

	}
		
	private double findWidth(Set<Center> centers)
	{
		double min = min(centers, comparingDouble(c -> c.loc.x)).loc.x;
		double max = max(centers, comparingDouble(c -> c.loc.x)).loc.x;
		return max - min;
	}
		
	private Set<Point> extractLocationsFromCenters(Set<Center> centers)
	{
		return centers.stream().map(c -> c.loc).collect(toCollection(TreeSet::new));
	}

	private Set<Point> extractLocationsFromCorners(Collection<Corner> corners)
	{
		return corners.stream().map(c -> c.loc).collect(toCollection(TreeSet::new));
	}

	/**
	 * For finding rivers.
	 */
	private List<River> findRivers(WorldGraph graph)
	{
		var rivers = new ArrayList<River>();
		var explored = new HashSet<Corner>();
		for (Edge edge : graph.edges)
		{
			if (edge.river >= riverMinWidth
					&& edge.v0 != null && edge.v1 != null
					&& !explored.contains(edge.v0) && !explored.contains(edge.v1))
			{
				River river = followRiver(edge.v0, edge.v1);
				
				// This count shouldn't be necessary. For some reason followRiver(...) is returning
				// rivers which contain many Corners already in explored.
				var count = 0;
				for (Corner c : river)
				{
					if (explored.contains(c))
						count++;
				}
				
				explored.addAll(river.getCorners());
				
				if (count < 3)
					rivers.add(river);
			}
		}
		
		return rivers;
	}
	
	/**
	 *  Searches along edges to find corners which are connected by a river. If the river forks, only
	 *	one direction is followed.
	 * @param last The search will not go in the direction of this corner.
	 * @param head The search will go in the direction of this corner.
	 * @return A set of corners which form a river.
	 */
	private River followRiver(Corner last, Corner head)
	{
		assert last != null;
		assert head != null;			
		assert !head.equals(last);
		
		River result = new River();
		result.add(head);
		result.add(last);
		
		Set<Edge> riverEdges = new TreeSet<>();
		for (Edge e : head.protrudes)
			if (e.river >= riverMinWidth)
				riverEdges.add(e);
		
		if (riverEdges.size() == 0)
		{
			throw new IllegalArgumentException("\"last\" should be connected to head by a river edge");
		}
		if (riverEdges.size() == 1)
		{
			// base case
			return result;
		}
		if (riverEdges.size() == 2)
		{
			// Find the other river corner which is not "last".
			Corner other = null;
			for (Edge e : riverEdges)
				if (head.equals(e.v0) && !last.equals(e.v1))
				{
					other = e.v1;
				}
				else if (head.equals(e.v1) && !last.equals(e.v0))
				{
					other = e.v0;
				}

			
			if (other == null)
			{
				// The only direction this river can go is to a null corner. This is a base case.
				return result;
			}

			result.addAll(followRiver(head, other));
		}
		else
		{
			// There are more than 2 river edges connected to head.
			
			// Sort the river edges by river width.
			List<Edge> edgeList = new ArrayList<>(riverEdges);
			edgeList.sort((e0, e1) -> -Integer.compare(e0.river, e1.river));
			Corner nextHead = null;
			
			// Find which edge contains "last".
			int indexOfLast = -1;
			for (int i : new Range(edgeList.size()))
			{
				if (last == edgeList.get(i).v0 || last == edgeList.get(i).v1)
				{
					indexOfLast = i;
					break;
				}
			}
			assert indexOfLast != -1;

			// Are there 2 edges which are wider rivers than all others?
			if (edgeList.get(1).river > edgeList.get(2).river)
			{
				
				// If last is one of those 2.
				if (indexOfLast < 2)
				{
					// nextHead = the other larger option.
					Edge nextHeadEdge;
					if (indexOfLast == 0)
					{
						nextHeadEdge = edgeList.get(1);
					}
					else
					{
						nextHeadEdge = edgeList.get(0);
					}
					
					if (!head.equals(nextHeadEdge.v0))
					{
						nextHead = nextHeadEdge.v0;
						assert nextHead != null;
					}
					else if (!head.equals(nextHeadEdge.v1))
					{
						nextHead = nextHeadEdge.v1;
						assert nextHead != null;
					}
					else
					{
						assert false; // Both corners cannot be the head.
					}
					
				}
				else
				{
					// This river is joining a larger river. This is a base case because the smaller
					// river should have a different name than the larger one.
					return result;
				}
			}
			else
			{
				// Choose the option with the largest river, avoiding choosing "last".
				edgeList.remove(indexOfLast);
				
				nextHead = head.equals(edgeList.get(0).v0) ? edgeList.get(0).v1 : edgeList.get(0).v0;
				assert nextHead != null;
			}
			// Leave the other options for the global search to hit later.
			
			result.addAll(followRiver(head, nextHead));
		}
		return result;
	}
	
	/**
	 * Draws the given name to the map with the area around the name drawn from landAndOceanBackground
	 * to make it readable when the name is drawn on top of mountains or trees.
	 */
	private void drawBackgroundBlendingForText(BufferedImage map, Graphics2D g, Point textStart, double angle, FontMetrics metrics, String text)
	{		
		int textWidth = metrics.stringWidth(text);
		int textHeight = getFontHeight(metrics);
		
		// This magic number below is a result of trial and error to get the blur levels to look right.
		int kernelSize = (int)((13.0 / 54.0) * textHeight);
		if (kernelSize == 0)
		{
			return;
		}
		int padding = kernelSize/2;
	
		BufferedImage textBG = new BufferedImage(textWidth + padding*2, textHeight + padding*2, BufferedImage.TYPE_BYTE_GRAY);
				
		Graphics2D bG = textBG.createGraphics();
		bG.setFont(g.getFont());
		bG.setColor(Color.white);
		bG.drawString(text, padding, padding + metrics.getAscent());
		
		// Use convolution to make a hazy background for the text.
		BufferedImage haze = ImageHelper.convolveGrayscale(textBG, ImageHelper.createGaussianKernel(kernelSize), true);
		// Threshold it and convolve it again to make the haze bigger.
		ImageHelper.threshold(haze, 1);
		haze = ImageHelper.convolveGrayscale(haze, ImageHelper.createGaussianKernel(kernelSize), true);
				
		ImageHelper.combineImagesWithMaskInRegion(map, landAndOceanBackground, haze, 
				((int)textStart.x) - padding, (int)(textStart.y) - metrics.getAscent() - padding, angle);
	}

	private void drawNameHorizontalAtPoint(Graphics2D g, String name, Point location, boolean boldBackground)
	{	
		if (name.length() == 0)
			return;
		
		Font original = g.getFont();
		Color originalColor = g.getColor();
		Font background = g.getFont().deriveFont(Font.BOLD, g.getFont().getSize());
		FontMetrics metrics = g.getFontMetrics(original);
		
		Point curLoc = new Point(location.x , location.y);
		for (int i : new Range(name.length()))
		{
			if (boldBackground)
			{
				g.setFont(background);
				g.setColor(settings.boldBackgroundColor);
				g.drawString("" + name.charAt(i), (int)curLoc.x, (int)curLoc.y);
				g.setFont(original);
				g.setColor(originalColor);
			}
			g.drawString("" + name.charAt(i), (int)curLoc.x, (int)curLoc.y);
			curLoc.x += metrics.stringWidth("" + name.charAt(i));
		}
	}

	private boolean drawNameHorizontal(BufferedImage map, Graphics2D g, String name, Set<Point> locations,
									   WorldGraph graph, boolean boldBackground, TextType textType)
	{
		if (name.length() == 0)
			return false;
		
		Point centroid = findCentroid(locations);
		
		centroid.y += 0;
		
		MapText text = createMapText(name, centroid, 0.0, textType);
		if (drawNameHorizontal(map, g, locations, graph, boldBackground, true, text))
		{
			mapTexts.add(text);
			return true;
		}
		if (locations.size() > 0)
		{
			// Try random locations to try to find a place to fit the text.
			Point[] locationsArray = locations.toArray(new Point[0]);
			for (@SuppressWarnings("unused") int i : new Range(30))
			{
				// Select a few random locations and choose the one closest to the centroid.
				List<Point> samples = new ArrayList<>(3);
				for (@SuppressWarnings("unused") int sampleNumber : new Range(5))
				{
					samples.add(locationsArray[r.nextInt(locationsArray.length)]);
				}
				
				Point loc = Helper.maxItem(samples, (point1, point2) -> -Double.compare(point1.distanceTo(centroid), point2.distanceTo(centroid)));

				text = createMapText(name, loc, 0.0, textType);
				if (drawNameHorizontal(map, g, locations, graph, boldBackground, true, text))
				{
					mapTexts.add(text);
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Draws the given name at the given location (centroid). If the name cannot be drawn on one line and still
	 * fit with the given locations, then it will be drawn on 2 lines.
	 * 
	 * @return True iff text was drawn.
	 */
	private boolean drawNameHorizontal(BufferedImage map, Graphics2D g, 
			Set<Point> locations, WorldGraph graph, boolean boldBackground,
			boolean enableBoundsChecking, MapText text)
	{	
		FontMetrics metrics = g.getFontMetrics(g.getFont());
		double width = metrics.stringWidth(text.value);
		double height = getFontHeight(metrics);
		Point textLocation = new Point(text.location.x * settings.resolution, text.location.y * settings.resolution);

		String[] parts = text.value.split(" ");
		
		if (parts.length > 1 && !locations.contains(graph.findClosestCenter((int)textLocation.x - width/2, (int)textLocation.y - height/2).loc))
		{		
			// One or more of the corners doesn't fit in the region. Draw it on 2 lines.
			int start = text.value.length()/2;
			int closestL = start;
			for (; closestL >= 0; closestL--)
				if (text.value.charAt(closestL) == ' ')
						break;
			int closestR = start;
			for (; closestR < text.value.length(); closestR++)
				if (text.value.charAt(closestR) == ' ')
						break;
			int pivot;
			if (Math.abs(closestL - start) < Math.abs(closestR - start))
				pivot = closestL;
			else
				pivot = closestR;
			String nameLine1 = text.value.substring(0, pivot);
			String nameLine2 = text.value.substring(pivot + 1);
			Point ulCorner1 = new Point(textLocation.x - metrics.stringWidth(nameLine1)/2.0,
					textLocation.y - getFontHeight(metrics)/2.0);
			Point ulCorner2 =  new Point(textLocation.x - metrics.stringWidth(nameLine2)/2.0,
					textLocation.y + getFontHeight(metrics)/2.0);
			
			// Make sure we don't draw on top of existing text. Only draw the text if both lines can be drawn.
			// Check line 1.
			java.awt.Rectangle bounds1 = new java.awt.Rectangle((int)ulCorner1.x, 
					(int)ulCorner1.y, metrics.stringWidth(nameLine1), getFontHeight(metrics));
			Area area1 = new Area(bounds1);
			if (enableBoundsChecking && overlapsExistingTextOrCityOrIsOffMap(area1))
			{
				return false;
			}
			// Check line 2.
			java.awt.Rectangle bounds2 = new java.awt.Rectangle((int)ulCorner2.x, 
					(int)ulCorner2.y, metrics.stringWidth(nameLine2), getFontHeight(metrics));
			Area area2 = new Area(bounds2);
			if (enableBoundsChecking && overlapsExistingTextOrCityOrIsOffMap(area2))
			{
				return false;
			}
			text.areas = Arrays.asList(area1, area2);

			Point textStartLine1 = new Point(ulCorner1.x, ulCorner1.y + metrics.getAscent());
			//drawBackgroundBlending(map, g, (int)bounds1.getWidth(), (int)bounds1.getHeight(), ulCorner1, 0);
			drawBackgroundBlendingForText(map, g, textStartLine1, 0, metrics, nameLine1);
			drawNameHorizontalAtPoint(g, nameLine1, textStartLine1, boldBackground);
			
			Point textStartLine2 = new Point(ulCorner2.x, ulCorner2.y + metrics.getAscent());
			//drawBackgroundBlending(map, g, (int)bounds2.getWidth(), (int)bounds2.getHeight(), ulCorner2, 0);
			drawBackgroundBlendingForText(map, g, textStartLine2, 0, metrics, nameLine2);
			drawNameHorizontalAtPoint(g, nameLine2, textStartLine2, boldBackground);
		}
		else
		{	
			// Make sure we don't draw on top of existing text.
			java.awt.Rectangle bounds = new java.awt.Rectangle((int)(textLocation.x - width/2),
					(int)(textLocation.y - height/2), metrics.stringWidth(text.value), (int)height);
			//g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
			Area area = new Area(bounds);
			if (enableBoundsChecking && overlapsExistingTextOrCityOrIsOffMap(area))
			{
				return false;
			}
			
			text.areas = singletonList(area);
			
			Point boundsLocation = new Point(bounds.getLocation().x, bounds.getLocation().y);
			
			Point textStart = new Point(boundsLocation.x, boundsLocation.y + metrics.getAscent());
			//drawBackgroundBlending(map, g, width, height, boundsLocation, 0);
			drawBackgroundBlendingForText(map, g, textStart, 0, metrics, text.value);
			drawNameHorizontalAtPoint(g, text.value, textStart, boldBackground);
		}
		return true;
	}
	
	public static java.awt.Point getTextBounds(String text, Font font)
	{
		FontMetrics metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics().getFontMetrics(font);
		return new java.awt.Point(metrics.stringWidth(text), 
				metrics.getHeight());
	}
	
	private static int getFontHeight(FontMetrics metrics)
	{
		return metrics.getAscent() + metrics.getDescent();
	}

	public void drawNameRotated(BufferedImage map, Graphics2D g, String name, Set<Point> locations, 
			boolean enableBoundsChecking, TextType type)
	{
		drawNameRotated(map, g, name, locations, 0.0, enableBoundsChecking, type);
	}

	/**
	 * Draws the given name at the centroid of the given plateCenters. The angle the name is
	 * drawn at is the least squares line through the plate centers. This does not break text
	 * into multiple lines.
	 * 
	 * Side effect: This adds a new MapText to mapTexts.
	 * 
	 * @param riseOffset The text will be raised (positive y) by this much distance above the centroid when
	 *  drawn. The rotation will be applied to this location. If there is already a name drawn above the object,
	 *  I try negating the riseOffset to draw the name below it. Positive y is down.
	 */
	public void drawNameRotated(BufferedImage map, Graphics2D g, String name, Set<Point> locations,
			double riseOffset, boolean enableBoundsChecking, TextType type)
	{
		if (name.length() == 0)
			return;
		
				
		Point centroid = findCentroid(locations);
		
		SimpleRegression regression = new SimpleRegression();
		for (Point p : locations)
		{
			regression.addObservation(new double[]{p.x}, p.y);
		}
		double angle;
		try
		{
			regression.regress();
			
			// Find the angle to rotate the text to.
			double y0 = regression.predict(0);
			double y1 = regression.predict(1);
			// Move the intercept to the origin.
			y1 -= y0;
			angle = Math.atan(y1);
		}
		catch(NoDataException e)
		{
			// This happens if the regression had only 2 or fewer points.
			angle = 0;
		}
				
		MapText text = createMapText(name, centroid, angle, type);
		if (drawNameRotated(map, g, riseOffset, enableBoundsChecking, text))
		{
			mapTexts.add(text);
		}
	}
	
	/**
	 * Draws the given name at the given location (centroid), at the given angle. This does not break text
	 * into multiple lines.
	 * @param riseOffset The text will be raised (positive y) by this much distance above the centroid when
	 *  drawn. The rotation will be applied to this location. If there is already a name drawn above the object,
	 *  I try negating the riseOffset to draw the name below it. Positive y is down.
	 *  
	 *  @return true iff the text was drawn.
	 */
	public boolean drawNameRotated(BufferedImage map, Graphics2D g,
			double riseOffset, boolean enableBoundsChecking, MapText text)
	{
		FontMetrics metrics = g.getFontMetrics(g.getFont());
		int width = metrics.stringWidth(text.value);
		int height = getFontHeight(metrics);
		
		if (width == 0 || height == 0)
		{
			// The text is too small to draw.
			return false;
		}
		
		Point textLocation = new Point(text.location.x * settings.resolution, text.location.y * settings.resolution);

		Point offset = new Point(riseOffset * Math.sin(text.angle), -riseOffset * Math.cos(text.angle));		
		Point pivot = new Point(textLocation.x - offset.x, textLocation.y - offset.y);
		
		AffineTransform orig = g.getTransform();
		g.rotate(text.angle, pivot.x, pivot.y);
		
		// Make sure we don't draw on top of existing text.
		java.awt.Rectangle bounds = new java.awt.Rectangle((int)(pivot.x - width/2), 
				(int)(pivot.y - height/2), width, height);
		Area area = new Area(bounds);
		area = area.createTransformedArea(g.getTransform());
		if (enableBoundsChecking && overlapsExistingTextOrCityOrIsOffMap(area))
		{
			// If there is a riseOffset, try negating it to put the name below the object instead of above.
			offset = new Point(-riseOffset * Math.sin(text.angle), riseOffset * Math.cos(text.angle));
	
			pivot = new Point(textLocation.x - offset.x, textLocation.y - offset.y);
			bounds = new java.awt.Rectangle((int)(pivot.x - width/2),
					(int)(pivot.y - height/2), width, height);
			
			g.setTransform(orig);
			g.rotate(text.angle, pivot.x, pivot.y);
			area = new Area(bounds);
			area = area.createTransformedArea(g.getTransform());
			if (overlapsExistingTextOrCityOrIsOffMap(area))
			{
				// Give up.
				//g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
				g.setTransform(orig);
				return false;
			}
		}
		text.areas = singletonList(area);
		// Update the text location with the offset.
		text.location = new Point(pivot.x / settings.resolution, pivot.y / settings.resolution);
		
		Point textStart = new Point(bounds.getLocation().x, bounds.getLocation().y + metrics.getAscent());
		drawBackgroundBlendingForText(map, g, textStart, text.angle, metrics, text.value);
		
		//g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
		
		g.drawString(text.value, (int) textStart.x, (int) textStart.y);
		g.setTransform(orig);
		
		return true;
	}
	
	private Set<Center> findPlateCentersWaterOnly(final TectonicPlate plate)
	{
		return plate.centers.stream().filter(c -> c.isWater).collect(toCollection(HashSet::new));
	}

	public Point findCentroid(Collection<Point> plateCenters)
	{
		Point centroid = new Point(0, 0);
		for (Point p : plateCenters)
		{
			centroid.x += p.x;
			centroid.y += p.y;
		}
		centroid.x /= plateCenters.size();
		centroid.y /= plateCenters.size();
		
		return centroid;
	}
	
	private boolean overlapsExistingTextOrCityOrIsOffMap(Area bounds)
	{
		for (MapText mp : mapTexts)
		{
			// Ignore empty text and ignore edited text.
			if (mp.value.length() > 0)
			{
				for (Area a : mp.areas)
				{
					Area aCopy = new Area(a);
					aCopy.intersect(bounds);
					if (!aCopy.isEmpty())
						return true;
				}
			}
		}
		
		for (Area a : cityAreas)
		{
			Area aCopy = new Area(a);
			aCopy.intersect(bounds);
			if (!aCopy.isEmpty())
				return true;
		}
		
		return !graphBounds.contains(bounds.getBounds2D());
	}
	
	/**
	 * Creates a new MapText, taking settings.resolution into account.
	 */
	private MapText createMapText(String text, Point location, double angle, TextType type)
	{
		return createMapText(text, location, angle, type, new ArrayList<>(0));
	}
	
	/**
	 * Creates a new MapText, taking settings.resolution into account.
	 */
	private MapText createMapText(String text, Point location, double angle, TextType type, List<Area> areas)
	{
		return new MapText(text, new Point(location.x / settings.resolution, location.y / settings.resolution), angle, type, areas);
	}

	/**
	 * If the given point lands within the bounding box of a piece of text, this
	 * returns the first one found. Else null is returned.
	 */
	public MapText findTextPicked(java.awt.Point point)
	{
		for (MapText mp : mapTexts)
		{
			if (mp.value.length() > 0)
				for (Area a : mp.areas)
				{
					if (a.contains(point))
						return mp;
				}
		}
		return null;
	}
	
	public void setSettings(MapSettings mapSettings)
	{
		this.settings = mapSettings;
	}

	/**
	 * Adds text that the user is manually creating.
	 */
	public MapText createUserAddedText(TextType type, Point location)
	{
		String name = generateNameOfTypeForTextEditor(type);
		// Getting the id must be done after calling generateNameOfType because said method increments textCounter
		// before generating the name.
		return createMapText(name, location, 0.0, type, new ArrayList<>(0));
	}
	

	public void setMapTexts(CopyOnWriteArrayList<MapText> text)
	{
		this.mapTexts = text;
	}
	
}
