package nortantis.util;

import java.io.Serializable;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class Counter <T extends Comparable<T>> implements Serializable
{
	private final Map<T, Integer> counts = new TreeMap<>();
	private int totalCount = 0;
	
	public void increment(T item)
	{
		counts.put(item, !counts.containsKey(item) ? 1 : counts.get(item) + 1);
		totalCount++;
	}
	
	public void add(T item, int count)
	{
		counts.put(item, !counts.containsKey(item) ? count : counts.get(item) + count);
		totalCount += count;		
	}
	
	public double get(T item)
	{
		return counts.get(item);
	}
	
	public T sample(Random r)
	{
		var uniformSample = r.nextInt(totalCount);

		double acc = 0;
		for (var entry : counts.entrySet()) {
			acc += entry.getValue();
			if (acc >= uniformSample)
				return entry.getKey();
		}

		throw new IllegalStateException("unable to find sample");
	}
}
