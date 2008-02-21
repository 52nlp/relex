package relex;
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

import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import relex.feature.FeatureNode;
import relex.feature.LinkableView;

/**
 * A ParsedSentence object stores all of the syntactic and semantic information
 * about a sentence parse. The data in the Object is gradually built up by
 * RelationExtractor
 *
 * ParsedSentence contains: 1. a FeatureNode with metaData about the parse (i.e.,
 * the number of conjunctions) 2. a ArrayList of FeatureNodes (leafConstituents)
 * representing each word in the sentence. -- the parse data can be found by
 * checking the links in these words. 3. Strings representing the original
 * sentence, and representations of its parses 4. Sets of relations, with the
 * semantic data from the sentence.
 */
public class ParsedSentence
{
	// String containing the original sentence
	private String original;

	// String containing the ascii-art tree output by the link grammar parser.
	private String linkString;

	// A string containing the Penn tree-bank style markup,
	// aka "phrase structure" markup, for example
	// (S (NP I) (VP am (NP a big robot)) .)
	private String phraseSring;

	private String errorString;

	// Metadata about the sentence; primarily, this consists of diagnostic
	// info returned by the link grammar parser.
	private FeatureNode metaData;

	// An ArrayList of FeatureNodes, each one representing a word in the
	// sentence.  If there are no "link islands", each can be reached by
	// following arcs from the others.
	private ArrayList<FeatureNode> leafConstituents;

	/* -------------------------------------------------------------------- */
	/* Constructors, and setters/getters for private members. */
	// Constructor.
	public ParsedSentence(String originalString) {
		original = originalString;
		linkString = null;
		errorString = "";
		phraseSring = null;
		leafConstituents = new ArrayList<FeatureNode>();
	}

	public void setMetaData(FeatureNode f) {
		metaData = f;
	}

	public FeatureNode getMetaData() {
		return metaData;
	}

	public String getOriginalSentence() {
		return original;
	}

	public String getLinkString() {
		return linkString;
	}

	public void setLinkString(String str) {
		linkString = str;
	}

	public String getPhraseString() {
		return phraseSring;
	}

	public void setPhraseString(String str) {
		phraseSring = str;
	}

	public void setErrorString(String eString) {
		errorString = eString;
	}

	public String getErrorString() {
		return errorString;
	}

	/* -------------------------------------------------------------------- */
	public int getNumWords() {
		return leafConstituents.size();
	}

	public FeatureNode getWordAsNode(int i) {
		return leafConstituents.get(i);
	}

	public String getWord(int i) {
		return LinkableView.getWordString(getWordAsNode(i));
	}

	public void addWord(FeatureNode w) {
		leafConstituents.add(w);
	}

	/* -------------------------------------------------------------------- */
	/* Various different views of the parsed sentence */

	/**
	 * Shows the full feature structure of the parse as it can be found by
	 * tracing links from the left-most word. Islands will be missed
	 */
	public String fullParseString() {
		if (getLeft() != null)
			return getLeft().toString();
		return "";
	}

	/**
	 * Returns a list of the words in the sentence, marked up according to
	 * which "part of speech" they are.  Thus, for example:
	 * "The big red baloon floated away." becomes
	 * LEFT-WALL The.det big.adj red.adj balloon.noun float.verb away.prep .
	 */
	public String printPartsOfSpeech()
	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < leafConstituents.size(); i++) {
			sb.append(getWord(i));
			LinkableView w = new LinkableView(getWordAsNode(i));
			String pos = w.getPOS();
			if (pos != null && !pos.equals("WORD"))
				sb.append("." + pos);
			String tense = w.getTenseVal();  // ??? tense is not working ...
			if (tense != null && tense.length() > 0)
				sb.append(tense);
			if (i < leafConstituents.size() - 1)
				sb.append(" ");
			// else
			// sb.append(".");
		}
		return sb.toString();
	}

	public String toString()
	{
		return original;
	}

	/* ---------------------------------------------------------------- */
	/**
	 * Returns an Iterator over ALL the FeatureNodes in the parse.
	 * That is, not only are nodes representing the constituents
	 * returned, but also all their sub-FeatureNodes representing
	 * links, semantic info, etc.
	 *
	 * @return an Iterator over ALL the FeatureNodes in the parse.
	 */
	public Iterator<FeatureNode> iteratorFromLeft()
	{
		return _iteratorFromLeft(getLeft(), new LinkedHashSet<FeatureNode>()).iterator();
	}

	private LinkedHashSet<FeatureNode> 
	_iteratorFromLeft(FeatureNode f, LinkedHashSet<FeatureNode> alreadyVisited) 
	{
		if (alreadyVisited.contains(f))
			return alreadyVisited;
		alreadyVisited.add(f);
		if (f.isValued())
			return alreadyVisited;
		Iterator<String> i = f.getFeatureNames().iterator();
		while (i.hasNext())
			_iteratorFromLeft(f.get(i.next()), alreadyVisited);
		return alreadyVisited;
	}

	/**
	 * @return the FeatureNode representing the left-most word in the sentence.
	 */
	public FeatureNode getLeft() {
		return leafConstituents.get(0);
	} // end getLeft

} // end ParsedSentence

