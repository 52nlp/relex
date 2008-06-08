/*
 * Copyright 2008 Novamente LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package relex;

import java.lang.String;
import relex.stats.Histogram;

/**
 * This class collects a miscellany of statistics about a parsed text.
 *
 * Copyright (C) 2008 Linas Vepstas <linas@linas.org> 
 */
public class ParseStats
{
	private int count;
	private Histogram parse_count;
	private Histogram word_count;
	private int failed_parses;
	private Histogram first_parse_confidence;
	private Histogram second_parse_confidence;
	private Histogram third_parse_confidence;

	public ParseStats()
	{
		count = 0;
		word_count = new Histogram(1,31);
		parse_count = new Histogram(0,10);
		failed_parses = 0;

		first_parse_confidence = new Histogram(20, 0.0, 1.0);
		second_parse_confidence = new Histogram(20, 0.0, 1.0);
		third_parse_confidence = new Histogram(20, 0.0, 1.0);
	}

	public void bin(RelexInfo ri)
	{
		count ++;
		int nparses = ri.parsedSentences.size();
		if (nparses <= 0) return;

		parse_count.bin(nparses);

		ParsedSentence fs = ri.parsedSentences.get(0);
		word_count.bin(fs.getNumWords());

		// If the first parse has skipped words, the parse is "failed"
		if (fs.getNumSkippedWords() != 0) failed_parses ++;

		// Count the first parse only if its "good"
		if (fs.getNumSkippedWords() == 0)
			first_parse_confidence.bin(fs.getTruthValue().getConfidence());

		if (2 <= nparses)
			second_parse_confidence.bin(ri.parsedSentences.get(1).getTruthValue().getConfidence());
		if (3 <= nparses)
			third_parse_confidence.bin(ri.parsedSentences.get(2).getTruthValue().getConfidence());

	}

	public String toString()
	{
		String str = "";
		str += "\nTotal sentences: " + count;
		str += "\nFailed parses: " + failed_parses;
		str += "\nWords per sentence: " + word_count.getMean();
		str += "\nParses per sentence: " + parse_count.getMean();
		str += "\nConfidence of first parse: " + first_parse_confidence.getMean() +
		       " of " + first_parse_confidence.getCount() + " parses";
		str += "\nConfidence of second parse: " + second_parse_confidence.getMean() +
		       " of " + second_parse_confidence.getCount() + " parses";
		str += "\nConfidence of third parse: " + third_parse_confidence.getMean() +
		       " of " + third_parse_confidence.getCount() + " parses";
		return str;
	}
}

