package nortantis;

import com.fasterxml.jackson.databind.ObjectMapper;
import nortantis.MapSettings.LineStyle;
import nortantis.MapSettings.OceanEffect;
import nortantis.util.AssetsPath;
import nortantis.util.Counter;
import nortantis.util.Range;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * For randomly generating settings with which to generate a map.
 *
 */
public class SettingsGenerator
{
	private static final Path defaultSettingsFile = AssetsPath.get("internal", "old_paper.json");
	public static int minWorldSize = 2000;
	public static int maxWorldSize = 30000;
	public static int worldSizePrecision = 1000;
	public static double maxCityProbability = 1.0/40.0;

	public static MapSettings generate(ObjectMapper mapper)
	{
		if (!Files.exists(defaultSettingsFile))
		{
			throw new IllegalArgumentException("The default settings files " + defaultSettingsFile + " does not exist");
		}

		var rand = new Random();
		// Prime the random number generator
		for (int i = 0; i < 100; i++)
		{
			rand.nextInt();
		}

		MapSettings settings;
		try {
			settings = mapper
					.reader()
					.readValue(defaultSettingsFile.toFile(), MapSettings.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		settings.pointPrecision = MapSettings.defaultPointPrecision;
		
		setRandomSeeds(settings, rand);

		var hueRange = 16;
		var saturationRange = 25;
		var brightnessRange = 25;

		var landColor = rand.nextInt(2) == 1 ? settings.landColor : settings.oceanColor;
		Color oceanColor;
		if (landColor == settings.landColor)
		{
			oceanColor = settings.oceanColor;
		}
		else
		{
			oceanColor = rand.nextInt(2) == 1 ? settings.landColor : settings.oceanColor;
		}
		settings.oceanEffect = new Counter<>(OceanEffect.values()).random(rand);
		settings.oceanEffectSize = 10 + Math.abs(rand.nextInt(40));
		settings.landBlur = 10 + Math.abs(rand.nextInt(40));
		
		settings.landColor = MapCreator.generateColorFromBaseColor(rand, landColor, hueRange, saturationRange, brightnessRange);
		
		settings.oceanColor = MapCreator.generateColorFromBaseColor(rand, oceanColor, hueRange, saturationRange, brightnessRange);

		var landBlurColorScale = 0.5;
		settings.landBlurColor = new Color((int)(settings.landColor.getRed() * landBlurColorScale), (int)(settings.landColor.getGreen() * landBlurColorScale), (int)(settings.landColor.getBlue() * landBlurColorScale));
		if (settings.oceanEffect == OceanEffect.Ripples)
		{
			settings.oceanEffectsColor = Color.black;
		}
		else if (settings.oceanEffect == OceanEffect.Blur)
		{
			var oceanEffectsColorScale = 0.3;
			settings.oceanEffectsColor = new Color((int)(settings.oceanColor.getRed() * oceanEffectsColorScale), (int)(settings.oceanColor.getGreen() * oceanEffectsColorScale), (int)(settings.oceanColor.getBlue() * oceanEffectsColorScale));
		}
		else
		{
			// Concentric waves
			var oceanEffectsColorScale = 0.5;
			var alpha = 255;
			settings.oceanEffectsColor = new Color((int)(settings.oceanColor.getRed() * oceanEffectsColorScale), (int)(settings.oceanColor.getGreen() * oceanEffectsColorScale), (int)(settings.oceanColor.getBlue() * oceanEffectsColorScale), alpha);
			
		}
		settings.riverColor = MapCreator.generateColorFromBaseColor(rand, settings.riverColor, hueRange, saturationRange, brightnessRange);
		settings.frayedBorderColor = MapCreator.generateColorFromBaseColor(rand, settings.frayedBorderColor, hueRange, saturationRange, brightnessRange);
		
		settings.worldSize = (rand.nextInt((maxWorldSize - minWorldSize) / worldSizePrecision) + minWorldSize / worldSizePrecision) * worldSizePrecision;
		
		settings.frayedBorder = rand.nextDouble() > 0.5;
		settings.frayedBorderBlurLevel = Math.abs(rand.nextInt(150));
		settings.frayedBorderSize = 100 + Math.abs(rand.nextInt(20000));
		
		settings.grungeWidth = 100 + rand.nextInt(1400);

		var drawBorderProbability = 0.25;
		settings.drawBorder = rand.nextDouble() > drawBorderProbability;
		var borderTypes = MapCreator.getAvailableBorderTypes();
		if (!borderTypes.isEmpty())
		{
			// Random border type.
			var index = Math.abs(rand.nextInt()) % borderTypes.size();
			settings.borderType = borderTypes.toArray(new String[0])[index];
			if (settings.borderType.equals("dashes"))
			{
				settings.frayedBorder = false;
				settings.borderWidth = Math.abs(rand.nextInt(50)) + 25;
			}
			else
			{
				settings.borderWidth = Math.abs(rand.nextInt(200)) + 100;
			}
		}
		
		if (rand.nextDouble() > 0.25)
		{
			settings.cityProbability =  0.25 * maxCityProbability;
		}
		else
		{
			settings.cityProbability = 0.0;
		}
		var cityIconSets = IconDrawer.getIconSets(IconType.cities);
		if (cityIconSets.size() > 0)
		{
			settings.cityIconSetName = new ArrayList<>(cityIconSets).get(rand.nextInt(cityIconSets.size()));
		}
		
		settings.drawRegionColors = rand.nextDouble() > 0.25;
		
		if (rand.nextDouble() > 0.5)
		{
			settings.generateBackground = true;
			settings.generateBackgroundFromTexture = false;
		}
		else
		{
			settings.generateBackground = false;
			settings.generateBackgroundFromTexture = true;

			var exampleTexturesPath = AssetsPath.get().resolve("example textures");
			List<Path> textureFiles;
			try
			{
				textureFiles = Files.list(exampleTexturesPath).filter(path -> !Files.isDirectory(path)).collect(Collectors.toList());
			}
			catch(IOException ex)
			{
				throw new RuntimeException("The example textures folder does not exist.", ex);
			}
			
			if (textureFiles.size() > 0)
			{
				settings.backgroundTextureImage = textureFiles.get(rand.nextInt(textureFiles.size()));
			}
		}
		
		settings.drawBoldBackground = rand.nextDouble() > 0.5;
		settings.boldBackgroundColor = MapCreator.generateColorFromBaseColor(rand, settings.boldBackgroundColor, hueRange, saturationRange, brightnessRange);
		
		// This threshold prevents large maps from having land on the edge, because such maps should be the entire world/continent.
		var noOceanOnEdgeThreshold = 15000.0;
		if (settings.worldSize < noOceanOnEdgeThreshold)
		{
			settings.edgeLandToWaterProbability = settings.worldSize / noOceanOnEdgeThreshold;
			// Make the edge and center land water probability add up to 1 so there is usually both land and ocean.
			settings.centerLandToWaterProbability = 1.0 - settings.edgeLandToWaterProbability;
		}
		else
		{
			settings.centerLandToWaterProbability = 0.5 + rand.nextDouble() * 0.5;
			settings.edgeLandToWaterProbability = 0;
		}
		
		settings.edgeLandToWaterProbability = Math.round(settings.edgeLandToWaterProbability * 100.0) / 100.0;
		settings.centerLandToWaterProbability = Math.round(settings.centerLandToWaterProbability * 100.0) / 100.0;

		var dimension = RunSwing.parseGenerateBackgroundDimensionsFromDropdown(RunSwing.getAllowedDimmensions().get(rand.nextInt(RunSwing.getAllowedDimmensions().size())));
		settings.generatedWidth = dimension.width;
		settings.generatedHeight = dimension.height;
		
		settings.books.clear();
		var allBooks = RunSwing.getAllBooks();
		if (allBooks.size() < 3)
		{
			settings.books.addAll(allBooks);
		}
		else
		{
			var numBooks = 2 + Math.abs(rand.nextInt(allBooks.size() - 1));
			var booksRemaining = new ArrayList<>(allBooks);
			for (@SuppressWarnings("unused") var ignored : new Range(numBooks))
			{
				var index = rand.nextInt(booksRemaining.size());
				settings.books.add(booksRemaining.get(index));
				booksRemaining.remove(index);
			}
		}
		
		settings.lineStyle = new Counter<>(LineStyle.values()).random(rand);
				
		return settings;
	}
	
	private static void setRandomSeeds(MapSettings settings, Random rand)
	{
		var seed = Math.abs(rand.nextInt());
		settings.randomSeed = seed;
		settings.regionsRandomSeed = seed;
		settings.backgroundRandomSeed = seed;
		settings.textRandomSeed = seed;
	}
}
