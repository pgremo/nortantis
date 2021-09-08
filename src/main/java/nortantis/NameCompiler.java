package nortantis;

import nortantis.util.AssetsPath;
import nortantis.util.Counter;
import nortantis.util.Pair;

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
	private final List<Pair<String, String>> nounAdjectivePairs;
	private List<Pair<String, String>> nounVerbPairs;
	// Used to decide whether to return a result from nounAdjectivePairs or nounVerbPairs.
	private final Counter<String> counter;
	private final Random r;
	public void setSeed(long seed)
	{
		r.setSeed(seed);
	}
	private final Set<String> dict;

	public NameCompiler(Random r, List<Pair<String, String>> nounAdjectivePairs,
			List<Pair<String, String>> nounVerbPairs)
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
		for (String line : lines)
		{
			String[] parts = line.split("[\\s0-9/]");
			if (parts.length == 0)
				continue;
			String word = parts[0];
			word = word.trim();
			dict.add(word);
		}

		this.nounVerbPairs = convertToPresentTense(nounVerbPairs);

		// Make all first letters capital.
		this.nounAdjectivePairs = capitalizeFirstLetters(nounAdjectivePairs);
		this.nounVerbPairs = capitalizeFirstLetters(this.nounVerbPairs);


		this.r = r;
		counter = new Counter<>();
		counter.add("adjectives", this.nounAdjectivePairs.size());
		counter.add("verbs", this.nounVerbPairs.size());
	}

	private List<Pair<String, String>> convertToPresentTense(List<Pair<String, String>> verbPairs)
	{
		return verbPairs.stream()
				.map(x -> new Pair<>(x.first(), convertVerbToPresentTense(x.second())))
				.collect(toList());
	}

	private List<Pair<String, String>> capitalizeFirstLetters(List<Pair<String, String>> pairs)
	{
		return pairs.stream()
				.map(x -> new Pair<>(capitalize(x.first()), capitalize(x.second())))
				.collect(toList());
	}

	public String compileName()
	{
		if (counter.random(r).equals("adjectives"))
		{
			if (nounAdjectivePairs.size() == 0)
			{
				return "";
			}
			var pair = nounAdjectivePairs.get(r.nextInt(nounAdjectivePairs.size()));
			double d = r.nextDouble();
			String result;
			if (d < 1.0/3.0)
			{
				// Just return the noun.
				result = pair.first();
			}
			else if (d < 2.0/3.0)
			{
				// Just return the adjective.
				result = pair.second();
			}
			else
			{
				// Return both.
				result = pair.second() + " " + pair.first();
			}
			return result;
		}
		else
		{
			if (nounVerbPairs.size() == 0)
			{
				return "";
			}
			var pair = nounVerbPairs.get(r.nextInt(nounVerbPairs.size()));
			double d = r.nextDouble();
			String result;
			if (d < 0.5)
			{
				// Just return the noun.
				result = pair.first();
			}
			else
			{
				// Return both.
				result = pair.second() + " " + pair.first();
			}

			return result;
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
		List<Character> vowels = Arrays.asList('a', 'e', 'i', 'o', 'u');

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
