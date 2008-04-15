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

import relex.ParsedSentence;
import relex.feature.FeatureNode;
import relex.feature.FeatureNodeCallback;
import relex.tree.PatternCallback;
import relex.tree.PatternMatch;
import relex.tree.PhraseTree;

/**
 * Identify "lexical units" aka "phrase chunks" aka "lexicalized stems".
 * These are identified by means of certain sentence patterns: certain 
 * "idiomatic" patterns for NP, VP and PP phrases.  The main function
 * is split into two parts: a dictionary of phrase types, and a pattern
 * matcher that can find these inside of sentences.
 *
 * Copyright (C) 2008 Linas Vepstas <linas@linas.org>
 */

public class PatternChunker extends LexicalChunker
{
	static final int debug = 0;

	private PatCB callback;
	
	public PatternChunker()
	{
		super();
		callback = new PatCB();
	}

	public void findChunks(ParsedSentence parse)
	{
		PhraseTree pt = parse.getPhraseTree();
		SubPhrase sb = new SubPhrase();
		pt.foreach(sb);
	}

	/* -------------------------------------------------------- */
	/* Try to pattern match each subphrase */
	private class SubPhrase implements FeatureNodeCallback
	{
		private PhraseTree pt;
		/**
		 * Called for each phrase in a parse.
		 * Add all parts of the phrase tree.
		 */
		public Boolean FNCallback(FeatureNode fn)
		{
			pt = new PhraseTree(fn);

			// A list of clauses to match, in disjunctive normal form.
			matcher("(NP (NP a) (PP a (NP r)))");
			matcher("(NP (NP (NP a) a) (PP r (NP r)))");

			matcher("(S (VP a (NP a)))");
			matcher("(SBAR a (S (NP a) (VP a (NP r))))");

			// Phrasal verbs: prepositonal verbs
			// e.g. (VP look (PP after (NP them))) " We look after tehm."
			matcher("(VP a (PP a (NP r)))");  // a subphrase of the below, which seems to cover most of it?
			matcher("(VP a (VP a (PP a (NP r))))"); 
			matcher("(VP a (VP a (PP a (S (VP r)))))");

			matcher("(VP a (NP r) (NP a))");
			matcher("(VP a (NP r) (PP a (NP *)))");
			// matcher("(VP a (NP r) (PP a (NP r)))");

			matcher("(VP r (NP a) (S (VP a (VP r))))");
			matcher("(VP r (NP a) (PP a (NP r (NP r))))");

			// Prepositional phrasal verbs with TWO prepositions:
			matcher("(VP notcop (PP a (NP postcop)) (PP a (NP r)))");
			matcher("(VP a (PP a) (PP a (NP r)) *)");

			matcher("(VP r (VP a (NP a) (S (VP a (VP r (NP r))))))");

			// Phrasal verbs: particle verbs.
			matcher("(S (NP r) (VP a (PRT a)) *)");
			matcher("(VP notcop (VP a (PRT a) *))");

			matcher("(VP a (PRT a) (PP a (NP *)))");

			// this one is rather loose XXXX
			// matcher("(VP a (PRT a) *)");

			// p means "accept only if its a pronoun"
			matcher("(VP a (NP p) (PRT a))");
			// XXX this needs to be narrowed ... 
			// matcher("(VP a (NP p) (PRT a) *)");
			matcher("(VP a (NP r) (PRT a) (PP a (NP *)))");

			// matcher("");

			return false;
		}

		private void matcher(String str)
		{
			PatternMatch.match(str, pt, callback);
		}
	}

	/* -------------------------------------------------------- */

	private static void chunkWords(FeatureNode fn, Chunk chunk)
	{
		while (fn != null)
		{
			FeatureNode wd = fn.get("phr-word");
			if (wd != null) chunk.addWord(wd);

			// Quit when a subprhase is seen.
			FeatureNode subf = fn.get("phr-head");
			if (subf != null) return;

			fn = fn.get("phr-next");
		}
	}

	/* -------------------------------------------------------- */
	/* Use the phrase-tree approach to finding chunks */
	private class PatCB implements PatternCallback
	{
		private Chunk curr_chunk;
		private boolean saw_copula;
		public void FoundCallback(String pattern, PhraseTree pt)
		{
			if (0 < debug) System.out.println("========== match! "+ pattern + " == " + pt.toString());

			curr_chunk = new Chunk();
			add(curr_chunk);
			saw_copula = false;
		}
		public Boolean PMCallback(String pattern, PhraseTree pt)
		{
			if(0 < debug)
				System.out.println("=== >" + pattern + "< == >" + 
                              PhraseTree.toString(pt.getCursor()) +
				                  "< phr=" + pt.toString());
			if (pattern.equals("a"))
			{
				chunkWords(pt.getCursor(), curr_chunk);
			}

			// "p" means "accept only if its a pronoun".
			else if (pattern.equals("p"))
			{
				// Must have only one word in the phrase ... 
				if (1 != pt.getBreadth()) return false;

				// ... and that word must be a pronoun.
				FeatureNode word = pt.getFirstWord();
				if (false == isPronoun(word)) return false;
				chunkWords(pt.getCursor(), curr_chunk);
			}

			// Reject if its a copula (is, was, ...) and keep otherwise.
			else if (pattern.equals("notcop"))
			{
				FeatureNode word = pt.getFirstWord();
				if (isCopula(word))
				{
					saw_copula = true;
					return false;
				}
				chunkWords(pt.getCursor(), curr_chunk);
			}

			// Accept only if copula seen previously
			else if (pattern.equals("postcop"))
			{
				if (!saw_copula) return false;
				chunkWords(pt.getCursor(), curr_chunk);
			}
			return false;
		}

		// Return true if the feature node is a pronoun.
		private boolean isPronoun(FeatureNode fn)
		{
			fn = fn.get("ref");
			if (fn == null) return false;
			fn = fn.get("PRONOUN-FLAG");
			if (fn == null) return false;
			return true;
		}

		// Return true if the featue node is the verb "to be".
		private boolean isCopula(FeatureNode fn)
		{
			fn = fn.get("morph");
			if (fn == null) return false;
			fn = fn.get("root");
			if (fn == null) return false;
			String root = fn.getValue();
			if (root.equals("be")) return true;
			return false;
		}
	}

}
