package nortantis.util;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class ProbabilityHelper
{
	/**
	 * Samples a categorical distribution
	 */
	public static <T> T sampleCategorical(Random rand, List<Tuple2<Double, T>> distribution)
	{
		if (distribution.size() == 0)
		{
			throw new IllegalArgumentException("The distribution must have at least one value");
		}

		if (distribution.size() == 1)
		{
			return distribution.get(0).getSecond();
		}

		double totalWeight = distribution.stream().mapToDouble(Tuple2::getFirst).sum();
		if (totalWeight == 0)
		{
			throw new IllegalArgumentException("Total weight cannot be 0.");
		}
		double sample = rand.nextDouble() * totalWeight;
		double curWeight = 0;
		for (var tuple : distribution)
		{
			curWeight += tuple.getFirst();
			if (curWeight >= sample)
			{
				return tuple.getSecond();
			}
		}

		// This shouldn't actually happen.
		assert false;
		return distribution.get(distribution.size() - 1).getSecond();
	}

	/**
	 * Samples a uniform distribution over the given items
	 */
	public static <T> T sampleUniform(Random rand, List<T> items)
	{
		if (items.isEmpty())
		{
			throw new IllegalArgumentException("The distribution must have at least one value");
		}

		return items.get(rand.nextInt(items.size()));
	}

	public static <T extends Enum<T>> T sampleEnumUniform(Random rand, Class<T> enumType)
	{
		return sampleUniform(rand, asList(enumType.getEnumConstants()));
	}

	@SuppressWarnings("rawtypes")
	public static List<Tuple2<Double, Enum>> createUniformDistributionOverEnumValues(Enum[] values)
	{
		return Arrays.stream(values).map(value -> new Tuple2<>(1.0, value)).collect(toList());
	}

	public static void main(String[] args)
	{
		Map<String, Integer> counts = new HashMap<>();
		for (@SuppressWarnings("unused") int i : new Range(10000))
		{
			String value = sampleCategorical(new Random(), asList(
					 new Tuple2<>(0.1, "first"),
					 new Tuple2<>(0.5, "second"),
					 new Tuple2<>(0.4, "third")));
			if (!counts.containsKey(value))
			{
				counts.put(value, 0);
			}
			counts.put(value, counts.get(value) + 1);
		}
		System.out.println(counts);
	}
}
