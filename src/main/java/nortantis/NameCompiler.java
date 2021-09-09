package nortantis;

import nortantis.util.AssetsPath;
import nortantis.util.Counter;
import nortantis.util.Tuple2;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.text.WordUtils.capitalize;

/**
 * Creates names for rivers and mountains by putting nouns, verbs, and adjectives together.
 * @author joseph
 *
 */
public class NameCompiler
{
	// The first part of each pair is the noun.
	private final List<Tuple2<String, String>> nounAdjectiveTuples;
	private List<Tuple2<String, String>> nounVerbTuples;
	// Used to decide whether to return a result from nounAdjectivePairs or nounVerbPairs.
	private final Counter<String> counter;
	private final Random r;
	public void setSeed(long seed)
	{
		r.setSeed(seed);
	}
	private final Set<String> dict;

	public NameCompiler(Random r, List<Tuple2<String, String>> nounAdjectiveTuples,
			List<Tuple2<String, String>> nounVerbTuples)
	{
		// Load the word dictionary.
		List<String> lines;
		try
		{
			lines = Files.readAllLines(AssetsPath.get("internal","en_GB.dic"), Charset.defaultCharset());
		} catch (IOException e)
		{
			throw new RuntimeException("Unable to read word dictionary file.", e);
		}
		dict = new TreeSet<>();
		for (var line : lines)
		{
			var parts = line.split("[\\s0-9/]");
			if (parts.length == 0) continue;
			var word = parts[0];
			word = word.trim();
			dict.add(word);
		}

		this.nounVerbTuples = convertToPresentTense(nounVerbTuples);

		// Make all first letters capital.
		this.nounAdjectiveTuples = capitalizeFirstLetters(nounAdjectiveTuples);
		this.nounVerbTuples = capitalizeFirstLetters(this.nounVerbTuples);


		this.r = r;
		counter = new Counter<>();
		counter.add("adjectives", this.nounAdjectiveTuples.size());
		counter.add("verbs", this.nounVerbTuples.size());
	}

	private List<Tuple2<String, String>> convertToPresentTense(List<Tuple2<String, String>> tuples)
	{
		return tuples.stream()
				.map(x -> new Tuple2<>(x.first(), convertVerbToPresentTense(x.second())))
				.collect(toList());
	}

	private List<Tuple2<String, String>> capitalizeFirstLetters(List<Tuple2<String, String>> tuples)
	{
		return tuples.stream()
				.map(x -> new Tuple2<>(capitalize(x.first()), capitalize(x.second())))
				.collect(toList());
	}

	public String compileName()
	{
		if (counter.random(r).equals("adjectives"))
		{
			if (nounAdjectiveTuples.size() == 0)
			{
				return "";
			}
			var pair = nounAdjectiveTuples.get(r.nextInt(nounAdjectiveTuples.size()));
			var d = r.nextDouble();
			if (d < 1.0/3.0)
			{
				// Just return the noun.
				return pair.first();
			}
			else if (d < 2.0/3.0)
			{
				// Just return the adjective.
				return pair.second();
			}
			else
			{
				// Return both.
				return pair.second() + " " + pair.first();
			}
		}
		else
		{
			if (nounVerbTuples.size() == 0)
			{
				return "";
			}
			var pair = nounVerbTuples.get(r.nextInt(nounVerbTuples.size()));
			var d = r.nextDouble();
			if (d < 0.5)
			{
				// Just return the noun.
				return pair.first();
			}
			else
			{
				// Return both.
				return pair.second() + " " + pair.first();
			}
		}
	}

	/**
	 * Use rules from http://www.oxforddictionaries.com/us/words/verb-tenses-adding-ed-and-ing
	 * and some rules I made to convert a verb to present tense.
	 * @param verb to convert
	 * @return converted verb
	 */
	String convertVerbToPresentTense(String verb)
	{
		var vowels = Arrays.asList('a', 'e', 'i', 'o', 'u');

		if (verb.endsWith("ing"))
			return verb;

		if (verb.endsWith("ee") || verb.endsWith("ye") || verb.endsWith("oe"))
		{
			// Keep silent e.
			return verb + "ing";
		}

		if (verb.endsWith("ed"))
		{
			return verb.substring(0, verb.length() - 2) + "ing";
		}

		if (verb.endsWith("aid"))
		{
			return verb.substring(0, verb.length() - 2) + "ying";
		}

		if (verb.endsWith("ood"))
		{
			return verb.substring(0, verb.length() - 3) + "anding";
		}

		if (verb.endsWith("ave"))
		{
			return verb.substring(0, verb.length() - 3) + "iving";
		}

		if (verb.endsWith("een"))
		{
			return verb.substring(0, verb.length() - 1) + "ing";
		}

		if (verb.endsWith("e") && dict.contains(verb.substring(0, verb.length() - 1) + "ing"))
		{
			return verb.substring(0, verb.length() - 1) + "ing";
		}

		if (verb.endsWith("ought"))
		{
			if (dict.contains(verb + "ing"))
			{
				return verb + "ing";
			}
			// Give up.
			return verb;
		}

		if (verb.length() >= 3 &&
				!vowels.contains(verb.charAt(verb.length() - 1)) && vowels.contains(verb.charAt(verb.length() - 2))
				&& vowels.contains(verb.charAt(verb.length() - 3)))
		{
			// 2 vowels vowels by a consonant.
			return verb + "ing";
		}

		if (verb.endsWith("c"))
		{
			return verb + "king";
		}

		if (verb.length() >= 2 &&
				!vowels.contains(verb.charAt(verb.length() - 1)) && vowels.contains(verb.charAt(verb.length() - 2)))
		{
			// Use a massive dictionary to determine if I should double the consonant.
			if (dict.contains(verb + "ing"))
					return verb + "ing";
			char consonant = verb.charAt(verb.length() - 1);
			if (dict.contains(verb + consonant + "ing"))
					return verb + consonant + "ing";
			// Give up.
			return verb;
		}

		if (verb.endsWith("ept"))
		{
			return verb.substring(0, verb.length() - 2) + "eping";
		}

		if (dict.contains(verb + "ing"))
			return verb + "ing";
		// Give up.
		return verb;
	}

}
