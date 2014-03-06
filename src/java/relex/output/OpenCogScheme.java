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

package relex.output;

import java.util.ArrayList;

import relex.Document;
import relex.ParsedSentence;
import relex.Sentence;
import relex.anaphora.history.SentenceHistory;
import relex.feature.FeatureNode;

/**
 * The OpenCogScheme object outputs a ParsedSentence in the
 * OpenCog-style Scheme format. The actual format used, and its rationale,
 * is described in greater detail in the README file in the opencog
 * source code directory src/nlp/wsd/README.
 *
 * As the same sentence can have multiple parses, this class only
 * displays a single, particular parse.
 *
 * Copyright (c) 2007, 2008, 2013 Linas Vepstas <linas@linas.org>
 */
public class OpenCogScheme
{
	private ParsedSentence _parse = null;
	private String orig_sentence = null;

	private OpenCogSchemeLink link_scheme;
	private OpenCogSchemeRel rel_scheme;
	private OpenCogSchemeAnaphora anaphora_scheme;
	private boolean do_show_linkage = false;
	private boolean do_show_relex = false;
	private boolean do_show_anaphora = false;

	/* -------------------------------------------------------------------- */
	/* Constructors, and setters/getters for private members. */
	public OpenCogScheme()
	{
		rel_scheme = new OpenCogSchemeRel();
		link_scheme = new OpenCogSchemeLink();
		anaphora_scheme = new OpenCogSchemeAnaphora();
		orig_sentence = "";
	}

	public void setShowLinkage(boolean t) { do_show_linkage = t; }
	public boolean getShowLinkage() { return do_show_linkage; }

	public void setShowRelex(boolean t) { do_show_relex = t; }
	public boolean getShowRelex() { return do_show_relex; }

	public void setShowAnaphora(boolean flag) { do_show_anaphora = flag; }
	public boolean getShowAnaphora() { return do_show_anaphora; }

	public void setParse(ParsedSentence parse)
	{
		_parse = parse;

		orig_sentence += printWords();
		orig_sentence += printParse();
		orig_sentence += printSentence();

		link_scheme.setParse(_parse);
		rel_scheme.setParse(_parse);

		anaphora_scheme.clear();
		anaphora_scheme.setSentence(_parse);
	}

	/* -------------------------------------------------------------------- */
	public String toString()
	{
		String ret = "";

		ret += orig_sentence;

		if (do_show_linkage) ret += linkSchemeToString();
		if (do_show_relex) ret += relSchemeToString();
		if (do_show_anaphora) ret += anaphoraSchemeToString();

		// Don't repeat the orig sentence, until we get a new sentence.
		orig_sentence = "";
		return ret;
	}

	public String anaphoraSchemeToString() {
		return anaphora_scheme.toString();
	}

	public String relSchemeToString() {
		return rel_scheme.toString();
	}

	public String linkSchemeToString() {
		return link_scheme.toString();
	}

	/* -------------------------------------------------------------------- */

	/**
	 * Print the word instances of the original sentence, associating
	 * each instance to its WordNode.
	 */
	public String printWords()
	{
		String str = "";

		FeatureNode fn = _parse.getLeft();
		while (fn != null)
		{
			String word = fn.get("orig_str").getValue();
			String guid_word = fn.get("uuid").getValue();
			if (word.equals("LEFT-WALL"))
				word = "###LEFT-WALL###";
                        if(word.matches("[-+]?[0-9]*?\\.?[0-9]+"))
                        {
                            str += "(ReferenceLink (stv 1.0 1.0)\n" +
			       "   (WordInstanceNode \"" + guid_word + "\")\n" +
			       "   (NumberNode " + word + ")\n" +
			       ")\n";                            
                        } 
                        else 
                        {
                           str += "(ReferenceLink (stv 1.0 1.0)\n" +
			       "   (WordInstanceNode \"" + guid_word + "\")\n" +
			       "   (WordNode \"" + word + "\")\n" +
			       ")\n"; 
                        }

			

			str += "(WordInstanceLink (stv 1.0 1.0)\n" +
			       "   (WordInstanceNode \"" + guid_word + "\")\n" +
			       "   (ParseNode \"" + _parse.getIDString() + "\")\n" +
			       ")\n";

			fn = fn.get("NEXT");
		}
		return str;
	}

	/**
	 * Print the words in the parse, as made up out of word instances,
	 * maintaining the proper word order in the sentence.
	 */
	public String printParse()
	{
		String str = "(ReferenceLink (stv 1.0 1.0)\n" +
		             "   (ParseNode \"" + _parse.getIDString() + "\")\n" +
		             "   (ListLink\n";

		FeatureNode fn = _parse.getLeft();
		fn = fn.get("NEXT"); // skip LEFT-WALL
		while (fn != null)
		{
			String guid = fn.get("uuid").getValue();
			str += "      (WordInstanceNode \"" + guid + "\")\n";
			fn = fn.get("NEXT");
		}

		str += "   )\n" +
		       ")\n";
		return str;
	}

	/**
	 * Print a parseLink that attaches a specific parse to the sentence
	 * that its a part of.  Attach a truth value that represents the 
	 * parse ranking for the parse.
	 */
	public String printSentence()
	{
		Double confidence = _parse.getTruthValue().getConfidence();

		// Why the fuck is string formating so fucked in Java? WTF?
		String scf = confidence.toString();
		int strl = Math.min(6, scf.length());
		scf = scf.substring(0, strl);

		String str = "(ParseLink (stv 1 1)\n" +
		             "   (ParseNode \"" + _parse.getIDString() +
                             "\"(stv 1.0 " + scf + "))\n" +
		             "   (SentenceNode \"" + _parse.getSentence().getID() + "\")\n" +
		             ")\n";
		return str;
	}

	/**
	 * Print the original document, as made up out of sentences,
	 * maintaining the proper sentence order in the document.
	 */
	public String printDocument(Document doco)
	{
		String str = "(ReferenceLink (stv 1.0 1.0)\n" +
		             "   (DocumentNode \"" + doco.getID() + "\")\n" +
		             "   (ListLink\n";

		ArrayList<Sentence> sentence_list = doco.getSentences();
		for (int i=0; i<sentence_list.size(); i++)
		{
			str += "      (SentenceNode \"" +
			       sentence_list.get(i).getID() + "\")\n";
		}

		str += "   )\n" +
		       ")\n";
		return str;
	}

	/**
	 * Sets the history for the AnaphoraResolutionScheme
	 * @param history the history to be used for anaphora resolution
	 */
	public void setAnaphoraHistory(SentenceHistory history)
	{
		anaphora_scheme.setHistory(history);
	}

} // end OpenCogScheme
