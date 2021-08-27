package nortantis;

import hoten.voronoi.Center;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a political region on the map.
 * @author joseph
 *
 */
public class Region
{
	private final Set<Center> centers;
	public Set<Center> getCenters() { return Collections.unmodifiableSet(centers); }
	public int id;
	public Color backgroundColor;
	
	public Region()
	{
		this.centers = new HashSet<>();
	}
	
	public void addAll(Collection<Center> toAdd)
	{
		for (Center c : toAdd)
		{
			addAndSetRegion(c);
		}
	}
	
	public void removeAll(Collection<Center> toRemove)
	{
		for (Center c : toRemove)
		{
			remove(c);
		}
	}
	
	public void clear()
	{
		for (Center c : centers)
		{
			c.region = null;		
		}
		centers.clear();
	}
	
	public void addAndSetRegion(Center c)
	{
		centers.add(c);
		c.region = this;		
	}
	
	public int size() { return centers.size(); }
	
	public void remove(Center c)
	{
		centers.remove(c);
		if (c.region == this)
		{
			c.region = null;
		}
	}
	
	public boolean contains(Center c)
	{
		return centers.contains(c);
	}

	@Override
	public int hashCode()
	{
		return id;
	}
}
