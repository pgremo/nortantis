package nortantis;

import nortantis.nlp.CharacterNGram;
import nortantis.util.Range;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.max;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.text.WordUtils.capitalize;

public class NameGenerator
{
	private final CharacterNGram nGram;
	double averageWordLength;
	double maxWordLengthComparedToAverage;
	private final double probabilityOfKeepingNameLength1;
	private final double probabilityOfKeepingNameLength2;
	private final double probabilityOfKeepingNameLength3;
	private final Random rand;
	private final Set<String> romanNumerals;
	
	/**
	 * @param maxWordLengthComparedToAverage Any name generated which contains a word (separated by spaces) which is longer than
	 * maxWordLengthComparedToAverage * averageWordLength will be rejected.
	 * @param probabilityOfKeepingNameLength1 With this probability, words generated with length 1 will be rejected and another sample will be attempted.
	 * @param probabilityOfKeepingNameLength2 With this probability, words generated with length 2 will be rejected and another sample will be attempted.
	 * @param probabilityOfKeepingNameLength3 With this probability, words generated with length 3 will be rejected and another sample will be attempted.
	 */
	public NameGenerator(Random r, List<String> placeNames, double maxWordLengthComparedToAverage, 
			double probabilityOfKeepingNameLength1, double probabilityOfKeepingNameLength2, double probabilityOfKeepingNameLength3)
	{
		this.maxWordLengthComparedToAverage = maxWordLengthComparedToAverage;
		this.probabilityOfKeepingNameLength1 = probabilityOfKeepingNameLength1;
		this.probabilityOfKeepingNameLength2 = probabilityOfKeepingNameLength2;
		this.probabilityOfKeepingNameLength3 = probabilityOfKeepingNameLength3;
		nGram = new CharacterNGram(r, 3);
		rand = r;
		
		// Find the average word length.
		int sum = 0;
		int count = 0;
		for (String name : placeNames)
		{
			sum += name.length();
			count ++;
		}
		averageWordLength = ((double)sum)/count;
		
		// Convert all words to lower case.
		for (int i : new Range(placeNames.size()))
		{
			placeNames.set(i, placeNames.get(i).toLowerCase());
		}
		
		nGram.addData(placeNames);

		String romanNumeralString = "I,II,III,IV,V,VI,VII,VIII,IX,X,XI,XII,XIII,XIV,XV,XVI,XVII,XVIII,XIX,XX";
		romanNumerals = new HashSet<>(asList(romanNumeralString.split(",")));
	}

	public String generateName() throws NotEnoughNamesException
	{		
		String name;
		String longestWord;
		do
		{
			name = nGram.generateNameNotInCorpora();
			longestWord = max(asList(name.split(" ")), comparingInt(String::length));
		}
		while ((longestWord.length() > averageWordLength * maxWordLengthComparedToAverage) || isTooShort(name));
		// Capitalize first letter of generated names, including for multi-word names.
		name = capitalize(name);
		name = capitalizeRomanNumerals(name);
	
		return name;
	}
	
	private boolean isTooShort(String name)
	{
		double v = rand.nextDouble();
		switch (name.length()) {
			case 1:
				return v > probabilityOfKeepingNameLength1;
			case 2:
				return v > probabilityOfKeepingNameLength2;
			case 3:
				return v > probabilityOfKeepingNameLength3;
			default:
				return false;
		}
	}

	private String capitalizeRomanNumerals(String str)
	{
		return stream(str.split(" "))
				.map(s -> romanNumerals.contains(s.toUpperCase()) ? s.toUpperCase() : s)
				.collect(joining(" "));
	}
}
