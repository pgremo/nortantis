package nortantis;

import nortantis.editor.MapEdits;
import nortantis.util.AssetsPath;

import java.awt.*;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Set;

/**
 * For parsing and storing map settings.
 *
 * @author joseph
 */
public class MapSettings implements Serializable {
    public static final double defaultPointPrecision = 2.0;

    public long randomSeed;
    /**
     * A scalar multiplied by the map height and width to get the final resolution.
     */
    public double resolution;
    public int landBlur;
    public int oceanEffectSize;
    public OceanEffect oceanEffect = OceanEffect.Ripples;
    public int worldSize;
    public Color riverColor;
    public Color roadColor = Color.BLACK;
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
    public boolean colorizeOcean = true; // For backgrounds generated from a texture.
    public boolean colorizeLand = true; // For backgrounds generated from a texture.
    public Path backgroundTextureImage = AssetsPath.get("example textures");
    public long backgroundRandomSeed;
    public Color oceanColor;
    public Color landColor;
    public int generatedWidth;
    public int generatedHeight;
    public float fractalPower;
    public String landBackgroundImage;
    public String oceanBackgroundImage;
    public int hueRange = 16;
    public int saturationRange = 20;

    public int brightnessRange = 25;
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
    public MapEdits edits = new MapEdits();
    public boolean drawBoldBackground = true;
    public boolean drawRegionColors;
    public long regionsRandomSeed;
    public boolean drawBorder;
    public String borderType = "";
    public int borderWidth;
    public int frayedBorderSize = 10000;
    public boolean drawIcons = true;
    public boolean drawRivers = true; // Not saved
    public boolean drawRoads = true;
    public double cityProbability;
    public LineStyle lineStyle = LineStyle.Jagged;
    public String cityIconSetName = IconDrawer.getIconSets(IconDrawer.citiesName)
            .stream()
            .findFirst().orElse("");
    public double pointPrecision = 10.0; // Not exposed for editing. Only for backwards compatibility so I can change it without braking older settings files that have edits.

    public enum LineStyle {
        Jagged,
        Smooth
    }

    public enum OceanEffect {
        Blur,
        Ripples,
        ConcentricWaves,
    }
}
