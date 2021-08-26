package hoten.voronoi;

import hoten.geom.Point;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class VoronoiGraphTest 
{

	static void drawTriangleElevationWithXAndYGradientTest()
	{
		BufferedImage image = new BufferedImage(101,101, BufferedImage.TYPE_INT_RGB);
		Corner corner1 = new Corner();
		corner1.loc = new Point(0, 0);
		corner1.elevation = 0.0;
		Corner corner2 = new Corner();
		corner2.elevation = 0.5;
		corner2.loc = new Point(100, 0);
		Center center = new Center(new Point(100, 100));
		center.elevation = 1.0;
		Graphics2D g = image.createGraphics();
		VoronoiGraph.drawTriangleElevation(g, corner1, corner2, center);
		assertEquals(
				0,
				new Color(image.getRGB((int)corner1.loc.x, (int)corner1.loc.y)).getBlue());
		assertEquals(
				125,
				new Color(image.getRGB((int)corner2.loc.x - 1, (int)corner2.loc.y)).getBlue());
		assertEquals(
				251,
				new Color(image.getRGB((int)center.loc.x - 1, (int)center.loc.y - 2)).getBlue());
	}

	static void drawTriangleElevationZeroXGradientTest()
	{
		BufferedImage image = new BufferedImage(101,101, BufferedImage.TYPE_INT_RGB);
		Corner corner1 = new Corner();
		corner1.loc = new Point(0, 0);
		corner1.elevation = 0.5;
		Corner corner2 = new Corner();
		corner2.elevation = 0.5;
		corner2.loc = new Point(50, 0);
		Center center = new Center(new Point(50, 100));
		center.elevation = 1.0;
		Graphics2D g = image.createGraphics();
		VoronoiGraph.drawTriangleElevation(g, corner1, corner2, center);
		assertEquals(
				(int)(corner1.elevation * 255),
				new Color(image.getRGB((int)corner1.loc.x, (int)corner1.loc.y)).getBlue());
		assertEquals(
				(int)(corner2.elevation * 255),
				new Color(image.getRGB((int)corner2.loc.x - 1, (int)corner2.loc.y)).getBlue());
		assertEquals((int)(center.elevation * 253),
				new Color(image.getRGB((int)center.loc.x - 1, (int)center.loc.y - 2)).getBlue());
	}

	static void drawTriangleElevationZeroYGradientTest()
	{
		BufferedImage image = new BufferedImage(101,101, BufferedImage.TYPE_INT_RGB);
		Corner corner1 = new Corner();
		corner1.loc = new Point(0, 0);
		corner1.elevation = 0.0;
		Corner corner2 = new Corner();
		corner2.elevation = 0.0;
		corner2.loc = new Point(0, 100);
		Center center = new Center(new Point(50, 100));
		center.elevation = 1.0;
		Graphics2D g = image.createGraphics();
		VoronoiGraph.drawTriangleElevation(g, corner1, corner2, center);
		assertEquals(
				(int)(corner1.elevation * 255),
				new Color(image.getRGB((int)corner1.loc.x, (int)corner1.loc.y)).getBlue());
		assertEquals(
				(int)(corner2.elevation * 255),
				new Color(image.getRGB((int)corner2.loc.x, (int)corner2.loc.y)).getBlue());
		assertEquals((int)(center.elevation * 249),
				new Color(image.getRGB((int)center.loc.x - 1, (int)center.loc.y-1)).getBlue());
	}

	/**
	 * Unit test for findHighestZ.
	 */
	static void findHighestZTest()
	{
		Vector3D v1 = new Vector3D(0, 0, -3);
		Vector3D v2 = new Vector3D(0, 0, 1);
		Vector3D v3 = new Vector3D(0, 0, 2);

		List<Vector3D> list = Arrays.asList(v1, v2, v3);

		Collections.shuffle(list);

		assertEquals(v3, VoronoiGraph.findHighestZ(list.get(0), list.get(1), list.get(2)));
	}

	@Test
	public void runPrivateUnitTests() 
	{
		findHighestZTest();
		drawTriangleElevationZeroXGradientTest();
		drawTriangleElevationZeroYGradientTest();
		drawTriangleElevationWithXAndYGradientTest();
	}
	
}
