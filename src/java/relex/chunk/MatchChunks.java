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

package relex.chunk;

import java.util.ArrayList;

import relex.ParsedSentence;
import relex.feature.FeatureNode;
import relex.feature.FeatureNodeCallback;
import relex.tree.PatternCallback;
import relex.tree.PatternMatch;
import relex.tree.PhraseTree;

/**
 * Discover phrase chunks
 *
 * Copyright (C) 2008 Linas Vepstas <linas@linas.org>
 */

public class MatchChunks
{
	private ArrayList<Chunk> chunks;
	private PatCB callback;
	
	public MatchChunks()
	{
		chunks = new ArrayList<Chunk>();
		callback = new PatCB();
	}

	public void findChunks(ParsedSentence parse)
	{
		PhraseTree pt = parse.getPhraseTree();
		SubPhrase sb = new SubPhrase();
		pt.foreach(sb);
	}

	public ArrayList<Chunk> getChunks()
	{
		return chunks;
	}

	public void clear()
	{
		chunks.clear();
	}

	/* -------------------------------------------------------- */
	/* Try to pattern match each subphrase */
	private class SubPhrase implements FeatureNodeCallback
	{
		/**
		 * Called for each phrase in a parse.
		 * Add all parts of the phrase tree.
		 */
		public Boolean FNCallback(FeatureNode fn)
		{
			PhraseTree pt = new PhraseTree(fn);

			PatternMatch.match("(NP (NP a) (PP a (NP r)))", pt, callback);

			return false;
		}
	}

	/* -------------------------------------------------------- */

/* --
	private void chunkPhrase(FeatureNode fn, Chunk chunk)
	{
		fn = fn.get("phr-head");
		while (fn != null)
		{
			FeatureNode wd = fn.get("phr-word");
			if (wd != null) chunk.addWord(wd);

			// Add subphrases to the word list
			FeatureNode subf = fn.get("phr-head");
			if (subf != null) 
			{
				chunkPhrase(fn, chunk);
			}
			fn = fn.get("phr-next");
		}
	}
-- */

	/* -------------------------------------------------------- */
	/* Use the phrase-tree approach to finding chunks */
	private class PatCB implements PatternCallback
	{
		public Boolean PMCallback(String pattern, PhraseTree pt)
		{
System.out.println("duude " + pattern + " " + pt.toString());
			return false;
		}
	}

}
