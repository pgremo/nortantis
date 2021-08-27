package nortantis.nlp;

import nortantis.NotEnoughNamesException;
import nortantis.util.ListCounterMap;
import nortantis.util.Range;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Used to generate words using character level n-grams.
 * @author joseph
 *
 */
public class CharacterNGram
{
	private final int n;
	private final Random r;
	private final ListCounterMap<Character> lcMap;
	private Set<String> namesFromCorpora;
	
	private final char startToken = 0;
	private final char endToken = 4;
	
	/**
	 * 
	 * @param r The randomizer
	 * @param n The size of the n-grams. For bi-grams n=2, for tri-grams n=3, etc.
	 */
	public CharacterNGram(Random r, int n)
	{
		this.n = n;
		this.r = r;
		this.lcMap = new ListCounterMap<>();
	}
	
	public void addData(Collection<String> phrases)
	{
		for (var phrase : phrases)
		{
			for (var i : new Range(phrase.length()))
			{
				var lastChars = new ArrayList<Character>(n - 1);
				for (int j = i - n + 1; j < i; j++)
				{
					lastChars.add(j < 0 ? startToken : phrase.charAt(j));
				}
				
				lcMap.incrementCount(lastChars, phrase.charAt(i));
			}
			// Add the end token.
			var lastChars = new ArrayList<Character>(n - 1);
			for (var j = phrase.length() - n + 1; j < phrase.length(); j++)
			{
				lastChars.add(j < 0 ? startToken : phrase.charAt(j));
			}
			lcMap.incrementCount(lastChars, endToken);
		}
		
		namesFromCorpora = new HashSet<>(phrases);
	}
	
	public String generateNameNotInCorpora() throws NotEnoughNamesException
	{
		final int maxRetries = 20;
		for (@SuppressWarnings("unused") int retry : new Range(maxRetries))
		{
			String name = generateName();
			if (name.length() < 2)
			{
				continue;
			}
			if (!namesFromCorpora.contains(name))
			{
				// This name never appeared in the corpora.
				return name;
			}
		}
		
		throw new NotEnoughNamesException();
	}
	
	private String generateName()
	{
		if (lcMap.size() == 0)
			throw new IllegalStateException("At least one book must be selected to generate text.");
		var lastChars = new ArrayList<Character>();
		for (@SuppressWarnings("unused") var i : new Range(n - 1))
		{
			lastChars.add(startToken);
		}
		
		var result = new StringBuilder();
		char next;
		do
		{
			next = lcMap.sampleConditional(r, lastChars);
			lastChars.remove(0);
			lastChars.add(next);
			if (next != endToken)
				result.append(next);
		}
		while(next != endToken);

		return result.toString();
	}
	
	public static void main(String[] args)
	{
		List<String> strs = Arrays.asList("yellow", "bannana", "yellowish", "corn", "corn and rice", "corn without rice", "yellow corn");
		CharacterNGram generator = new CharacterNGram(new Random(), 3);
		generator.addData(strs);
		IntStream.range(1, 10)
				.mapToObj(x -> generator.generateName())
				.forEach(System.out::println);
	}
}
