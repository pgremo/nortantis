package nortantis.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Used to store a conditional probability distribution, where the conditioned variables
 * are in a list.
 * @author joseph
 *
 */
public class ListCounterMap <T extends Comparable<T>> implements Serializable
{
	private final Map<List<T>, Counter<T>> map = new TreeMap<>((a, b) -> {
		for (var i = 0; i < Math.min(a.size(), b.size()); i++)
		{
			var c = a.get(i).compareTo(b.get(i));
			if (c < 0)
				return -1;
			if (c > 0)
				return 1;
		}

		// So far all elements are the same.
		return Integer.compare(a.size(), b.size());
	});
	
	public int size()
	{
		return map.size();
	}
	
	public void incrementCount(List<T> key, T value)
	{
		Counter<T> counter = map.get(key);
		if (counter == null)
		{
			counter = new Counter<T>();
			map.put(key, counter);
		}
		counter.increment(value);
	}
	
	public double getCount(List<T> key, T value)
	{
		Counter<T> counter = map.get(key);
		if (counter == null)
			return 0.0;
		return counter.get(value);
	}
	
	/**
	 * If the given key has been seen, this returns a sample from the possible values,
	 * treated as a probability distribution conditioned on the key. If the key has not
	 * been seen, this returns null.
	 */
	public T sampleConditional(Random r, List<T> key)
	{
		Counter<T> counter = map.get(key);
		return counter == null ? null : counter.sample(r);
	}
		
	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		for (Map.Entry<List<T>, Counter<T>> entry: map.entrySet())
		{
			List<T> key = entry.getKey();
			Counter<T> counter = entry.getValue();

			result.append("key: ").append(key).append("\n")
					.append("value: ").append(counter).append("\n\n");
		}
		return result.toString();
	}
	
	public static void main(String[] args)
	{
		ListCounterMap<Character> cMap = new ListCounterMap<>();
		Random r = new Random();

		List<Character> key = Arrays.asList('a', 'b');

		cMap.incrementCount(key, 'c');

		Character character = cMap.sampleConditional(r, key);

		System.out.println(character);
	}

}
