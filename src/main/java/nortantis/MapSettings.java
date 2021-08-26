package nortantis;

import hoten.geom.Point;
import nortantis.editor.CenterEdit;
import nortantis.editor.EdgeEdit;
import nortantis.editor.MapEdits;
import nortantis.editor.RegionEdit;
import nortantis.util.AssetsPath;
import nortantis.util.Helper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import static java.nio.file.Files.newInputStream;

/**
 * For parsing and storing map settings.
 * @author joseph
 *
 */
public class MapSettings implements Serializable
{
	public static final double defaultPointPrecision = 2.0;

	public long randomSeed;
	/**
	 *  A scalar multiplied by the map height and width to get the final resolution.
	 */
	public double resolution;
	public int landBlur;
	public int oceanEffectSize;
	public OceanEffect oceanEffect;
	public int worldSize;
	public Color riverColor;
	public Color roadColor;
	public Color landBlurColor;
	public Color oceanEffectsColor;
	public Color coastlineColor;
	public double centerLandToWaterProbability;
	public double edgeLandToWaterProbability;
	public boolean frayedBorder;
	public Color frayedBorderColor;
	public int frayedBorderBlurLevel;
	public int grungeWidth;
	
	/**
	 * This settings actually means fractal generated as opposed to generated from texture.
	 */
	public boolean generateBackground; // This means generate fractal background. It is mutually exclusive with generateBackgroundFromTexture.
	public boolean generateBackgroundFromTexture;
	public boolean transparentBackground; 
	public boolean colorizeOcean; // For backgrounds generated from a texture.
	public boolean colorizeLand; // For backgrounds generated from a texture.
	public Path backgroundTextureImage;
	public long backgroundRandomSeed;
	public Color oceanColor;
	public Color landColor;
	public int generatedWidth;
	public int generatedHeight;
	public float fractalPower;
	public String landBackgroundImage;
	public String oceanBackgroundImage;
	public int hueRange;
	public int saturationRange;
	
	public int brightnessRange;
	public boolean drawText;
	public boolean alwaysCreateTextDrawerAndUpdateLandBackgroundWithOcean; // Not saved
	public long textRandomSeed;
	public Set<String> books;
	public Font titleFont;
	public Font regionFont;
	public Font mountainRangeFont;
	public Font otherMountainsFont;
	public Font riverFont;
	public Color boldBackgroundColor;
	public Color textColor;
	public MapEdits edits;
	public boolean drawBoldBackground;
	public boolean drawRegionColors;
	public long regionsRandomSeed;
	public boolean drawBorder;
	public String borderType;
	public int borderWidth;
	public int frayedBorderSize;
	public boolean drawIcons = true;
	public boolean drawRivers = true; // Not saved
	public boolean drawRoads = true;
	public double cityProbability;
	public LineStyle lineStyle;
	public String cityIconSetName;
	public double pointPrecision = defaultPointPrecision; // Not exposed for editing. Only for backwards compatibility so I can change it without braking older settings files that have edits.
	
	public MapSettings()
	{
		edits = new MapEdits();
	}
	
	public Properties toPropertiesFile()
	{
		Properties result = new Properties();
		result.setProperty("randomSeed", randomSeed + "");
		result.setProperty("resolution", resolution + "");
		result.setProperty("landBlur", landBlur + "");
		result.setProperty("oceanEffects", oceanEffectSize + "");
		result.setProperty("oceanEffect", oceanEffect + "");
		result.setProperty("worldSize", worldSize + "");
		result.setProperty("riverColor", colorToString(riverColor));
		result.setProperty("roadColor", colorToString(roadColor));
		result.setProperty("landBlurColor", colorToString(landBlurColor));
		result.setProperty("oceanEffectsColor", colorToString(oceanEffectsColor));
		result.setProperty("coastlineColor", colorToString(coastlineColor));
		result.setProperty("edgeLandToWaterProbability", edgeLandToWaterProbability + "");
		result.setProperty("centerLandToWaterProbability", centerLandToWaterProbability + "");
		result.setProperty("frayedBorder", frayedBorder + "");
		result.setProperty("frayedBorderColor", colorToString(frayedBorderColor));
		result.setProperty("frayedBorderBlurLevel", frayedBorderBlurLevel + "");
		result.setProperty("grungeWidth", grungeWidth + "");
		result.setProperty("cityProbability", cityProbability + "");
		result.setProperty("lineStyle", lineStyle + "");
		result.setProperty("pointPrecision", pointPrecision + "");

		// Background image settings.
		result.setProperty("backgroundRandomSeed", backgroundRandomSeed + "");
		result.setProperty("generateBackground", generateBackground + "");
		result.setProperty("backgroundTextureImage", backgroundTextureImage.toString());
		result.setProperty("generateBackgroundFromTexture", generateBackgroundFromTexture + "");
		result.setProperty("transparentBackground", transparentBackground + "");
		result.setProperty("colorizeOcean", colorizeOcean + "");
		result.setProperty("colorizeLand", colorizeLand + "");
		result.setProperty("oceanColor", colorToString(oceanColor));
		result.setProperty("landColor", colorToString(landColor));
		result.setProperty("generatedWidth", generatedWidth + "");
		result.setProperty("generatedHeight", generatedHeight + "");
		result.setProperty("fractalPower", fractalPower + "");
		result.setProperty("landBackgroundImage", landBackgroundImage);
		result.setProperty("oceanBackgroundImage", oceanBackgroundImage);
		
		// Region settings
		result.setProperty("drawRegionColors", drawRegionColors + "");
		result.setProperty("regionsRandomSeed", regionsRandomSeed + "");
		result.setProperty("hueRange", hueRange + "");
		result.setProperty("saturationRange", saturationRange + "");
		result.setProperty("brightnessRange", brightnessRange + "");
		
		// Icon sets
		result.setProperty("cityIconSetName", cityIconSetName + "");

		result.setProperty("drawText", drawText + "");
		result.setProperty("textRandomSeed", textRandomSeed + "");
		result.setProperty("books", Helper.toStringWithSeparator(books, "\t"));
		result.setProperty("titleFont", fontToString(titleFont));
		result.setProperty("regionFont", fontToString(regionFont));
		result.setProperty("mountainRangeFont", fontToString(mountainRangeFont));
		result.setProperty("otherMountainsFont", fontToString(otherMountainsFont));
		result.setProperty("riverFont", fontToString(riverFont));
		result.setProperty("boldBackgroundColor", colorToString(boldBackgroundColor));
		result.setProperty("drawBoldBackground", drawBoldBackground + "");
		result.setProperty("textColor", colorToString(textColor));
		
		result.setProperty("drawBorder", drawBorder + "");
		result.setProperty("borderType", borderType);
		result.setProperty("borderWidth", borderWidth + "");
		result.setProperty("frayedBorderSize", frayedBorderSize + "");
		result.setProperty("drawIcons", drawIcons + "");
		result.setProperty("drawRoads", drawRoads + "");
		
		// User edits.
		result.setProperty("editedText", editedTextToJson());
		result.setProperty("centerEdits", centerEditsToJson());
		result.setProperty("regionEdits", regionEditsToJson());
		result.setProperty("edgeEdits", edgeEditsToJson());
		result.setProperty("hasIconEdits", edits.hasIconEdits + "");
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private String editedTextToJson()
	{
		JSONArray list = new JSONArray();
		for (MapText text : edits.text)
		{
			JSONObject mpObj = new JSONObject();	
			mpObj.put("text", text.value);
			mpObj.put("locationX", text.location.x);
			mpObj.put("locationY", text.location.y);
			mpObj.put("angle", text.angle);
			mpObj.put("type", text.type.toString());
			list.add(mpObj);
		}
		String json = list.toJSONString();
		return json;
	}
	
	
	@SuppressWarnings("unchecked")
	private String centerEditsToJson()
	{
		JSONArray list = new JSONArray();
		for (CenterEdit centerEdit : edits.centerEdits)
		{
			JSONObject mpObj = new JSONObject();	
			mpObj.put("isWater", centerEdit.isWater);
			mpObj.put("regionId", centerEdit.regionId);
			if (centerEdit.icon != null)
			{
				JSONObject iconObj = new JSONObject();
				iconObj.put("iconGroupId", centerEdit.icon.iconGroupId);
				iconObj.put("iconIndex", centerEdit.icon.iconIndex);
				iconObj.put("iconName", centerEdit.icon.iconName);
				iconObj.put("iconType", centerEdit.icon.iconType.toString());
				mpObj.put("icon", iconObj);
			}
			if (centerEdit.trees != null)
			{
				JSONObject treesObj = new JSONObject();
				treesObj.put("treeType", centerEdit.trees.treeType);
				treesObj.put("density", centerEdit.trees.density);
				treesObj.put("randomSeed", centerEdit.trees.randomSeed);
				mpObj.put("trees", treesObj);
			}
			list.add(mpObj);
		}
		String json = list.toJSONString();
		return json;
	}
	
	@SuppressWarnings("unchecked")
	private String regionEditsToJson()
	{
		JSONArray list = new JSONArray();
		for (RegionEdit regionEdit : edits.regionEdits.values())
		{
			JSONObject mpObj = new JSONObject();	
			mpObj.put("color", colorToString(regionEdit.color));
			mpObj.put("regionId", regionEdit.regionId);
			list.add(mpObj);
		}
		String json = list.toJSONString();
		return json;
	}
	
	@SuppressWarnings("unchecked")
	private String edgeEditsToJson()
	{
		JSONArray list = new JSONArray();
		for (EdgeEdit eEdit : edits.edgeEdits)
		{
			JSONObject mpObj = new JSONObject();	
			mpObj.put("riverLevel", eEdit.riverLevel);
			mpObj.put("index", eEdit.index);
			list.add(mpObj);
		}
		String json = list.toJSONString();
		return json;
	}	
		
	private String colorToString(Color c)
	{
		if (c != null)
		{
			return c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "," + c.getAlpha();
		}
		else
		{
			return "";
		}
	}
	
	private String fontToString(Font font)
	{
		return font.getFontName() + "\t" + font.getStyle() + "\t" + font.getSize();
	}
		
	public MapSettings(Path propertiesFilename)
	{
		final Properties props = new Properties();
		try
		{
			props.load(newInputStream(propertiesFilename));
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		// Load parameters from the properties file.
		
		randomSeed = getProperty("randomSeed", () -> Long.parseLong(props.getProperty("randomSeed")));
		resolution = getProperty("resolution", () -> Double.parseDouble(props.getProperty("resolution")));
		landBlur = getProperty("landBlur", () -> Integer.parseInt(props.getProperty("landBlur")));
		oceanEffectSize = getProperty("oceanEffects", () -> Integer.parseInt(props.getProperty("oceanEffects")));
		worldSize = getProperty("worldSize", () -> Integer.parseInt(props.getProperty("worldSize")));
		riverColor = getProperty("riverColor", () -> parseColor(props.getProperty("riverColor")));
		roadColor = getProperty("roadColor", () -> {
			String roadColorString = props.getProperty("roadColor");
			if (roadColorString == null || roadColorString.equals(""))
			{
				return Color.black;
			}
			return parseColor(roadColorString);
		});
		landBlurColor = getProperty("landBlurColor", () -> parseColor(props.getProperty("landBlurColor")));
		oceanEffectsColor = getProperty("oceanEffectsColor", () -> parseColor(props.getProperty("oceanEffectsColor")));
		coastlineColor = getProperty("coastlineColor", () -> parseColor(props.getProperty("coastlineColor")));
		oceanEffect = getProperty("addWavesToOcean", () -> {
			String str = props.getProperty("oceanEffect");
			if (str == null || str.equals(""))
			{
				// Try the old property name.
				String str2 = props.getProperty("addWavesToOcean");
				if (str2 == null || str2.equals(""))
				{
					return OceanEffect.Ripples;
				}
				return parseBoolean(str2) ? OceanEffect.Ripples : OceanEffect.Ripples;
			}
			return OceanEffect.valueOf(str);
		});
		centerLandToWaterProbability = getProperty("centerLandToWaterProbability", () -> Double.parseDouble(props.getProperty("centerLandToWaterProbability")));
		edgeLandToWaterProbability = getProperty("edgeLandToWaterProbability", () -> Double.parseDouble(props.getProperty("edgeLandToWaterProbability")));
		frayedBorder = getProperty("frayedBorder", () -> parseBoolean(props.getProperty("frayedBorder")));
		frayedBorderColor = getProperty("frayedBorderColor", () -> parseColor(props.getProperty("frayedBorderColor")));
		frayedBorderBlurLevel = getProperty("frayedBorderBlurLevel", () -> (int)(Integer.parseInt(props.getProperty("frayedBorderBlurLevel"))));
		grungeWidth = getProperty("grungeWidth", () -> {
			String str = props.getProperty("grungeWidth");
			return str == null ? 0 : (int)(Integer.parseInt(str));
		});
		cityProbability = getProperty("cityProbability", () -> {
			String str = props.getProperty("cityProbability");
			return str == null ? 0.0 : Double.parseDouble(str);
		});
		lineStyle = getProperty("lineStyle", () -> 
		{
			String str = props.getProperty("lineStyle");
			if (str == null || str.equals(""))
			{
				return LineStyle.Jagged;
			}
			return LineStyle.valueOf(str);
		});
		pointPrecision = getProperty("pointPrecision", () -> {
			String str = props.getProperty("pointPrecision");
			return (str == null || str == "") ? 10.0 : Double.parseDouble(str); // 10.0 was the value used before I made a setting for it.
		});
		
		
		// Background image stuff.
		generateBackground = getProperty("generateBackground", () -> parseBoolean(props.getProperty("generateBackground")));
		generateBackgroundFromTexture = getProperty("generateBackgroundFromTexture", () -> {
			String propString = props.getProperty("generateBackgroundFromTexture");
			if (propString == null)
			{
				return false;
			}
			return parseBoolean(propString);
		});
		transparentBackground = getProperty("transparentBackground", () -> {
			String propString = props.getProperty("transparentBackground");
			if (propString == null)
			{
				return false;
			}
			return parseBoolean(propString);
		});
		colorizeOcean = getProperty("colorizeOcean", () -> {
			String propString = props.getProperty("colorizeOcean");
			if (propString == null)
			{
				return true;
			}
			return parseBoolean(propString);
		});
		colorizeLand = getProperty("colorizeLand", () -> {
			String propString = props.getProperty("colorizeLand");
			if (propString == null)
			{
				return true;
			}
			return parseBoolean(propString);
		});
		backgroundTextureImage = getProperty("backgroundTextureImage", () -> {
			var result = props.getProperty("backgroundTextureImage");
			return result == null ? AssetsPath.get("example textures") : Path.of(result);
		});
		backgroundRandomSeed = getProperty("backgroundRandomSeed", () -> Long.parseLong(props.getProperty("backgroundRandomSeed")));
		oceanColor = getProperty("oceanColor", () -> parseColor(props.getProperty("oceanColor")));
		landColor = getProperty("landColor", () -> parseColor(props.getProperty("landColor")));
		generatedWidth = getProperty("generatedWidth", () -> Integer.parseInt(props.getProperty("generatedWidth")));
		generatedHeight = getProperty("generatedHeight", () -> Integer.parseInt(props.getProperty("generatedHeight")));
		fractalPower = getProperty("fractalPower", () -> Float.parseFloat(props.getProperty("fractalPower")));
		landBackgroundImage = getProperty("landBackgroundImage", () -> {
			String result = props.getProperty("landBackgroundImage");
			if (result == null)
				throw new NullPointerException();
			return result;
		});
		oceanBackgroundImage = getProperty("oceanBackgroundImage", () -> {
			String result = props.getProperty("oceanBackgroundImage");
			if (result == null)
				throw new NullPointerException();
			return result;
		});
		
		drawRegionColors = getProperty("drawRegionColors", () ->
		{
			String str = props.getProperty("drawRegionColors");
			return str == null ? true : parseBoolean(str);
		});
		regionsRandomSeed = getProperty("regionsRandomSeed", () ->
		{
			String str = props.getProperty("regionsRandomSeed");
			return str == null ? 0 : (long)Long.parseLong(str);			
		});
		hueRange = getProperty("hueRange", () -> 
		{
			String str = props.getProperty("hueRange");
			return str == null ? 16 : Integer.parseInt(str); // default value
		});
		saturationRange = getProperty("saturationRange", () -> 
		{
			String str = props.getProperty("saturationRange");
			return str == null ? 20 : Integer.parseInt(str); // default value
		});
		brightnessRange = getProperty("brightnessRange", () -> 
		{
			String str = props.getProperty("brightnessRange");
			return str == null ? 25 : Integer.parseInt(str); // default value
		});
		drawIcons = getProperty("drawIcons", () -> {
			String str = props.getProperty("drawIcons");
			return str == null || parseBoolean(str);
		});
		drawRoads= getProperty("drawRoads", () -> {
			String str = props.getProperty("drawRoads");
			return str == null || parseBoolean(str);
		});
		
		cityIconSetName = getProperty("cityIconSetName", () -> 
		{
			String setName;
			try
			{
				setName = props.getProperty("cityIconSetName");
			}
			catch(Exception ex)
			{
				setName = "";
			}
			
			if (setName == null || setName.isEmpty())
			{
				Set<String> sets = IconDrawer.getIconSets(IconDrawer.citiesName);
				if (sets.size() > 0)
				{
					setName = sets.iterator().next();
				}
				else
				{
					setName = "";
				}
			}
			return setName;
		});
	
		drawText = getProperty("drawText", () -> parseBoolean(props.getProperty("drawText")));
		textRandomSeed = getProperty("textRandomSeed", () -> Long.parseLong(props.getProperty("textRandomSeed")));
		books = new TreeSet<>(getProperty("books", () -> Arrays.asList(props.getProperty("books").split("\t"))));
		
		titleFont = getProperty("titleFont", () -> parseFont(props.getProperty("titleFont")));

		titleFont = getProperty("titleFont", () -> parseFont(props.getProperty("titleFont")));
		regionFont = getProperty("regionFont", () -> parseFont(props.getProperty("regionFont")));

		mountainRangeFont = getProperty("mountainRangeFont", () -> parseFont(props.getProperty("mountainRangeFont")));
		otherMountainsFont = getProperty("otherMountainsFont", () -> parseFont(props.getProperty("otherMountainsFont")));
		riverFont = getProperty("riverFont", () -> parseFont(props.getProperty("riverFont")));
		boldBackgroundColor = getProperty("boldBackgroundColor", () -> parseColor(props.getProperty("boldBackgroundColor")));
		drawBoldBackground = getProperty("drawBoldBackground", () -> {
			String value = props.getProperty("drawBoldBackground");
			if (value == null)
				return true; // default value
			return  parseBoolean(value);
		});
		textColor = getProperty("textColor", () -> parseColor(props.getProperty("textColor")));
		drawBorder = getProperty("drawBorder", () -> {
			String value = props.getProperty("drawBorder");
			if (value == null)
				return false; // default value
			return  parseBoolean(value);
		});
		borderType = getProperty("borderType", () -> {
			String result = props.getProperty("borderType");
			if (result == null)
				return "";
			return result;
		});
		borderWidth = getProperty("borderWidth", () -> {
			String value = props.getProperty("borderWidth");
			if (value == null)
			{
				return 0;
			}
			return Integer.parseInt(value);
		});
		frayedBorderSize = getProperty("frayedBorderSize", () -> {
			String value = props.getProperty("frayedBorderSize");
			if (value == null)
			{
				return 10000;
			}
			return Integer.parseInt(value);
		});
		
		edits = new MapEdits();
		// hiddenTextIds is a comma delimited list.
				
		edits.text = getProperty("editedText", () -> {
			String str = props.getProperty("editedText");
			if (str == null || str.isEmpty())
				return new CopyOnWriteArrayList<>();
			JSONArray array = (JSONArray) JSONValue.parse(str);
			CopyOnWriteArrayList<MapText> result = new CopyOnWriteArrayList<>();
			for (Object obj : array) {
				JSONObject jsonObj = (JSONObject) obj;
				String text = (String) jsonObj.get("text");
				Point location = new Point((Double) jsonObj.get("locationX"), (Double) jsonObj.get("locationY"));
				double angle = (Double) jsonObj.get("angle");
				TextType type = Enum.valueOf(TextType.class, ((String) jsonObj.get("type")).replace(" ", "_"));
				MapText mp = new MapText(text, location, angle, type);
				result.add(mp);
			}

			return result;
		});
		
		edits.centerEdits = getProperty("centerEdits", () -> {
			String str = props.getProperty("centerEdits");
			if (str == null || str.isEmpty())
				return new ArrayList<>();
			JSONArray array = (JSONArray) JSONValue.parse(str);
			List<CenterEdit> result = new ArrayList<>();
			int index = 0;
			for (Object obj : array)
			{
				JSONObject jsonObj = (JSONObject) obj;
				boolean isWater = (boolean) jsonObj.get("isWater");
				Integer regionId = jsonObj.get("regionId") == null ? null : ((Long) jsonObj.get("regionId")).intValue();

				CenterIcon icon = null;
				{
					JSONObject iconObj = (JSONObject)jsonObj.get("icon");
					if (iconObj != null)
					{
						String iconGroupId = (String)iconObj.get("iconGroupId");
						int iconIndex = (int)(long)iconObj.get("iconIndex");
						String iconName = (String)iconObj.get("iconName");
						CenterIconType iconType = CenterIconType.valueOf((String)iconObj.get("iconType"));
						icon = new CenterIcon(iconType, iconGroupId, iconIndex);
						icon.iconName = iconName;
					}
				}

				CenterTrees trees = null;
				{
					JSONObject treesObj = (JSONObject)jsonObj.get("trees");
					if (treesObj != null)
					{
						String treeType = (String)treesObj.get("treeType");
						double density = (Double)treesObj.get("density");
						long randomSeed = (Long)treesObj.get("randomSeed");
						trees = new CenterTrees(treeType, density, randomSeed);
					}
				}

				result.add(new CenterEdit(index, isWater, regionId, icon, trees));
				index++;
			}

			return result;
		});

		edits.regionEdits = getProperty("regionEdits", () -> {
			String str = props.getProperty("regionEdits");
			if (str == null || str.isEmpty())
				return new ConcurrentHashMap<>();
			JSONArray array = (JSONArray) JSONValue.parse(str);
			ConcurrentHashMap<Integer, RegionEdit> result = new ConcurrentHashMap<>();
			for (Object obj : array)
			{
				JSONObject jsonObj = (JSONObject) obj;
				Color color = parseColor((String)jsonObj.get("color"));
				int regionId = (int)(long)jsonObj.get("regionId");
				result.put(regionId, new RegionEdit(regionId, color));
			}

			return result;
		});
		
		edits.edgeEdits = getProperty("edgeEdits", () -> {
			String str = props.getProperty("edgeEdits");
			if (str == null || str.isEmpty())
				return new ArrayList<>();
			JSONArray array = (JSONArray) JSONValue.parse(str);
			List<EdgeEdit> result = new ArrayList<>();
			for (Object obj : array)
			{
				JSONObject jsonObj = (JSONObject) obj;
				int riverLevel = (int)(long)jsonObj.get("riverLevel");
				int index = (int)(long)jsonObj.get("index");
				result.add(new EdgeEdit(index, riverLevel));
			}

			return result;
		});
		
		edits.hasIconEdits = getProperty("hasIconEdits", () -> {
			String value = props.getProperty("hasIconEdits");
			if (value == null)
				return false; // default value
			return  parseBoolean(value);
		});
	}
	
	private static boolean parseBoolean(String str)
	{
		if (str == null)
			throw new NullPointerException();
		if (!(str.equals("true") || str.equals("false")))
			throw new IllegalArgumentException();
		return Boolean.parseBoolean(str);
	}
	
	private static Color parseColor(String str)
	{
		if (str == null)
			throw new NullPointerException("A color is null.");
		String[] parts = str.split(",");
		if (parts.length == 3)
		{
			return new Color(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
		}
		if (parts.length == 4)
		{
			return new Color(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
		}
		throw new IllegalArgumentException("Unable to parse color from string: " + str);
	}
	
	private static <T> T getProperty(String propName, Supplier<T> getter)
	{
		try
		{
			return getter.get();
		}
		catch (NullPointerException e)
		{
			throw new RuntimeException(String.format("Property \"%s\" is missing or cannot be read.", propName), e);
		}
		catch(NumberFormatException e)
		{
			if (e.getMessage().equals("null"))
				throw new RuntimeException(String.format("Property \"%s\" is missing.", propName), e);
			else
				throw new RuntimeException(String.format("Property \"%s\" is invalid.", propName), e);
		}
		catch (Exception e)
		{
			throw new RuntimeException(String.format("Property \"%s\" is invalid.", propName), e);
		}
	}

	public static Font parseFont(String str)
	{
		return Font.decode(str);
	}
	
	@Override
	public boolean equals(Object other)
	{
		MapSettings o = (MapSettings)other;
		return toPropertiesFile().equals(o.toPropertiesFile());
	}
	
	public enum LineStyle
	{
		Jagged,
		Smooth
	}

	public enum OceanEffect
	{
		Blur,
		Ripples,
		ConcentricWaves,
	}
}
