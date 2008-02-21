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
package relex.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import relex.feature.FeatureNode;
import relex.feature.LinkableView;
import relex.feature.SemanticView;
import relex.output.RawView;

public class EntityMaintainer
{
	/**
	  * This number is derived from the LinkParser dictionary -- 
	 * it only accepts suffixes up to "60." To increase this 
	 * number, you need to add more words to the Link parser 
	 * dictionary, like "genericID61, dateID61, etc."
	 */
	public static int MAX_NUM_ENTITIES = 60;

	// The original sentence string
	private String originalSentence;

	public String getOriginalSentence() {
		return originalSentence;
	}

	// The converted sentence is a copy of the original, 
	// except all entities are replaced with ID strings
	private String convertedSentence;

	public String getConvertedSentence() {
		return convertedSentence;
	}

	// An array of EntityInfos, ordered by their order in the sentence
	public ArrayList<EntityInfo> orderedEntityInfos;

	// Maps entity ID strings to EntityInfos
	private HashMap<String,EntityInfo> iDs2Entities;

	// Keeps track of the last entityIDIndex created
	int entityIDIndex;

	// Maps feature nodes to entity IDs
	// private HashMap<FeatureNode, String> featureNodes2EntityIDs;

	// A set of integer indexes of inserted whitespace characters
	private TreeSet<Integer> insertedWhitespaceCharIndexes;

	/**
	 * Returns true iff the character after the entity is legal.
	 * Only whitespace and certain punction are allowed.
	 */
	private boolean isLegalEntityFollowingString(String s)
	{
		if (s.length() == 0)
			return true;
		char c = s.charAt(0);
		if (Character.isWhitespace(c))
			return true;
		if (c == '.' || c == ',' || c == ';' || c == ':')
			return true;
		if (s.startsWith("'s ") || s.startsWith("' "))
			return true;
		return false;
	}

	/**
	 * Creates the converted sentence. The converted sentence 
	 * is a copy of the original, except all entities are 
	 * replaced with ID strings.
	 */
	private void createConvertedSentence()
	{
		if ((orderedEntityInfos == null) || (orderedEntityInfos.size() == 0))
			convertedSentence = originalSentence;
		convertedSentence = "";
		int curIndex = 0;

		// For each entity...
		for(EntityInfo eInfo : orderedEntityInfos)
		{
			if (eInfo.getFirstCharIndex() < curIndex)
			{
				// XXX The following input triggers this warning:
				// BY ALEXIS THOMSON, F.R.C.S.Ed.
				// with the problem being at the period after the S.
				// Ignore this, for now.
				System.err.println(
						"Error: Entity start is at an unexpected location:\n" +
				      "Sentence= " + originalSentence + "\n" +
				      "firt char= " + eInfo.getFirstCharIndex() +
				      " curidx= " + curIndex + "\n");
				continue;
			}

			// Copy the preceeding portion (if any) of the original string.
			convertedSentence += originalSentence.substring(curIndex, eInfo
					.getFirstCharIndex());

			// Insert leading white space before eInfo if it is 
			// not preceeded by white space.
			if (convertedSentence.length() > 0
					&& !Character.isWhitespace(convertedSentence
							.charAt(convertedSentence.length() - 1))) {
				convertedSentence += ' ';
				insertedWhitespaceCharIndexes.add(new Integer(convertedSentence.length() - 1));
			}

			// Insert the ID string of the entity.
			convertedSentence += makeID(eInfo);
			curIndex = eInfo.getLastCharIndex() + 1;

			// Insert trailing white space after eInfo if it is 
			// not followed by a legal string
			if (originalSentence.length() > curIndex
					&& !isLegalEntityFollowingString(originalSentence
							.substring(curIndex))) {
				convertedSentence += ' ';
				insertedWhitespaceCharIndexes.add(new Integer(convertedSentence
						.length() - 1));
			}
		}

		// Copy the remaining portion (if any) of the original string.
		convertedSentence += originalSentence.substring(curIndex);
		// System.out.println("CONVERSION\n" + originalSentence + "\n" +
		// convertedSentence);
	}

	private String makeID(EntityInfo eInfo)
	{
		++entityIDIndex;
		String id = eInfo.idStringPrefix() + entityIDIndex;
		iDs2Entities.put(id, eInfo);
		return id;
	}

/*
 * -------------------- unused  --------------
	private Iterator<FeatureNode>
	recursiveFNIterator(FeatureNode f, ArrayList<FeatureNode> alreadyVisited) {
		if (alreadyVisited.contains(f))
			return alreadyVisited.iterator();
		alreadyVisited.add(f);
		if (f.isValued())
			return alreadyVisited.iterator();
		Iterator<String> i = f.getFeatureNames().iterator();
		while (i.hasNext())
			recursiveFNIterator(f.get(i.next()), alreadyVisited);
		return alreadyVisited.iterator();
	}
 * -------------------- unused  --------------
 */

	/*
	 * CONSTRUCTOR
	 */
	public EntityMaintainer(String _originalSentence,
			ArrayList<EntityInfo> _orderedEntityInfos)
	{
		if (_orderedEntityInfos.size() > MAX_NUM_ENTITIES) {
			System.err.println("WARNING: Sentence had more than "
					+ MAX_NUM_ENTITIES
					+ ".  Ignoring extras.\nOriginal sentence:"
					+ originalSentence);
		}
		originalSentence = _originalSentence;
		orderedEntityInfos = new ArrayList<EntityInfo>();

		for (EntityInfo it : _orderedEntityInfos)
		{
			orderedEntityInfos.add(it);
		}

		iDs2Entities = new HashMap<String, EntityInfo>();
		entityIDIndex = 0; // the first used index will be '1'

		insertedWhitespaceCharIndexes = new TreeSet<Integer>();
		createConvertedSentence();
	}

	public boolean isEntityID(String e) {
		return iDs2Entities.containsKey(e);
	}

	public EntityInfo getEntityInfo(String entityID) {
		return iDs2Entities.get(entityID);
	}

	/*
	 * Replaces all feature values containing entityIDs, so that they now
	 * contain original strings. Stores the FeatureNodes in a map which maps
	 * them to entityIDs Does not repair char index values in the word nodes
	 *
	 * XXX Deprecated, use repairSentence below.
	 */
/* DEAD CODE DEAD CODE
	private void recursivelyReplaceEntityIDsWithStrings(FeatureNode fn,
			boolean useNormalized) {
		Iterator<FeatureNode> allNodes = recursiveFNIterator(fn, new ArrayList<FeatureNode>());
		while (allNodes.hasNext()) {
			FeatureNode curFN = allNodes.next();
			if (curFN.isValued()) {
				String fnVal = curFN.getValue();
				if (isEntityID(fnVal)) {
					String id = fnVal;
					// replace id with original string
					if (useNormalized)
						curFN.setValue(getEntityInfo(id).getNormalizedString());
					else
						curFN.setValue(getEntityInfo(id).getOriginalString());
					featureNodes2EntityIDs.put(curFN, id);
				}
			}
		}
	}
DEAD CODE DEAD CODE */

	private int previouslyInsertedWhitespace(int index)
	{
		int insertedSoFar = 0;
		Iterator<Integer> inserts = insertedWhitespaceCharIndexes.iterator();
		while (inserts.hasNext()) {
			if (inserts.next().intValue() < index)
				++insertedSoFar;
		}
		return insertedSoFar;
	}

	/**
	 * Replaces all feature values containing entityIDs, so that they now
	 * contain original strings. Stores the FeatureNodes in a map which maps
	 * them to entityIDs. Does not repair char index values in the word nodes
	 *
	 * An alternative to recursivelyReplaceEntityIDsWithStrings Saves time by
	 * travering only the words and their refs. Also repairs start and end chars
	 */
	public void repairSentence(FeatureNode leftNode)
	{
		int charDelta = 0;
		try {
		for (LinkableView word = new LinkableView(leftNode); word != null; word = (word
				.getNext() == null ? null : new LinkableView(word.getNext())))
		{
			int previousWhiteSpace = previouslyInsertedWhitespace(word
					.getStartChar());
			String wordName = word.getWordString();
			word.setStartChar(word.getStartChar() + charDelta
					- previousWhiteSpace);
			// word.setExpandedStartChar(word.getExpandedStartChar()+charDelta);
			if (isEntityID(wordName)) {
				EntityInfo entInfo = getEntityInfo(wordName);
				String origName = entInfo.getOriginalString();
				charDelta += origName.length() - wordName.length();
				try {
					SemanticView semView = new SemanticView(word.fn()
							.get("ref"));
					semView.setName(origName);// .replaceAll("\\s","_"))
				} catch (Exception e) {
				}
			}
			word.setEndChar(word.getEndChar() + charDelta - previousWhiteSpace);
			// word.setExpandedEndChar(word.getExpandedEndChar()+charDelta);
		}
		}
		catch (Exception e)
		{
			System.err.println("Error: failed to repair sentence.");
			System.err.println("Borken sentence was:");
			System.err.println(RawView.printZHeads(leftNode));
		}
	}

	/**
	 * prepareSentence() -- markup parsed sentence with entity 
	 * information. This needs to be done before the relex algs run,
	 * as the relex algs may use some of this information.
	 */
	public void prepareSentence(FeatureNode leftNode)
	{
		for (LinkableView word = new LinkableView(leftNode); word != null; word = (word
				.getNext() == null ? null : new LinkableView(word.getNext())))
		{
			String wordName = word.getWordString();
			if (isEntityID(wordName)) {
				EntityInfo entInfo = getEntityInfo(wordName);
				entInfo.setProperties(word.fn());
			}
		}
	}

	public static void main(String[] args)
	{
		/*
		 * Arg0: a sentence with an entity Arg1: the first character of the
		 * entity Arg2: the last character of the entity Example:
		 * java relex.entity.EntityMaintainer "Does Mike think it will work?" 5 8
		 * Expected output: Does genericID1 think it will work?
		 */
		String sentence = args[0];
		ArrayList<EntityInfo> list = new ArrayList<EntityInfo>();
		int arg = 1;
		while (args.length >= arg + 2) {
			EntityInfo eInfo = new EntityInfo(sentence,
								   Integer.parseInt(args[arg]),
								   Integer.parseInt(args[arg + 1]));
			list.add(eInfo);
			arg += 2;
		}
		EntityMaintainer em = new EntityMaintainer(sentence, list);
		System.out.println(em.getConvertedSentence());
	}
}
