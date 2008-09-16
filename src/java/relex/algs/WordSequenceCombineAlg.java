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

package relex.algs;

/**
 * This algorithm combines sequences of words which should be a single word 
 * (proper names, and idioms like "at hand")
 */
import java.util.regex.Pattern;

import relex.concurrent.RelexContext;
import relex.feature.FeatureNode;
import relex.feature.LinkView;
import relex.feature.LinkableView;

public class WordSequenceCombineAlg extends TemplateMatchingAlg
{
	static String nameLabelRegex = "G[a-z\\*]*"; // a-z or '*'

	static String entityOrIdiomLabelRegex = "ID[A-Z]*[a-z\\*]*";

	static String allLabelRegex = "(" + nameLabelRegex + ")|(" + entityOrIdiomLabelRegex + ")";

	static int directionLeft = -1;

	static int directionRight = 1;

	/*
	 * Finds the next node
	 */
	FeatureNode nextNode(FeatureNode node, int dir, String labelRegex)
	{
		LinkableView linkable = new LinkableView(node);

 		// Iterate on right links.
		for (int i = 0; i < linkable.numLinks(dir); i++)
		{
			LinkView link = new LinkView(linkable.getLink(dir, i));
			String label = link.getLabel(0);
			if (Pattern.matches(labelRegex, label))
				return (dir == directionLeft ? link.getLeft() : link.getRight());
		}
		return null;
	}

	boolean isRightMost(FeatureNode node, String labelRegex)
	{
		return nextNode(node, directionRight, labelRegex) == null;
	}

	FeatureNode getLeftMost(FeatureNode node, String labelRegex)
	{
		FeatureNode next = nextNode(node, directionLeft, labelRegex);
		if (next == null)
			return node;
		return getLeftMost(next, labelRegex);
	}

	String collectNames(FeatureNode current, FeatureNode rightNode,
	                    String labelRegex, boolean shouldEraseStrAndRef)
	{
		String name = current.get("str").getValue();

		// Use the original string in case it has been changed
		if ((current.get("orig_str") != null) &&
		    (current.get("orig_str").equals("") == false))
		{
			name = current.get("orig_str").getValue();
		}

		if (shouldEraseStrAndRef && current != rightNode)
		{
			// Nodes should *NOT* actually be deleted, especially not the 
			// "orig_str". This is because it is needed for printing in
			// some of the output formats, which need to have access to
			// the original, unprocessed sentence.
			current.set("ref", null); // .set("name",new FeatureNode(""));

			current.set("str", new FeatureNode(""));
			// current.set("orig_str", new FeatureNode(""));
		}

		if (current == rightNode)
			return name;
		return name + "_"
				+ collectNames(nextNode(current, directionRight, labelRegex),
		                     rightNode, labelRegex, shouldEraseStrAndRef);
	}

	protected void applyTo(FeatureNode node, RelexContext context)
	{
		FeatureNode rightNode = getTemplate().val("right");
		if (rightNode != LinkView.getRight(node))
			throw new RuntimeException("variable 'right' is not properly assigned");
		if (!isRightMost(rightNode, allLabelRegex))
			return;

		FeatureNode leftNode = getLeftMost(rightNode, allLabelRegex);

		String bigName = collectNames(leftNode, rightNode, allLabelRegex, true);
		rightNode.get("ref").set("name", new FeatureNode(bigName));
		
		// See email message "Relex fixes: definites and conjoined 
		// prepositions" from Mike Ross
		// rightNode.get("ref").set("specific", new FeatureNode("T"));
		
		rightNode.set("str", new FeatureNode(bigName)); // bigNameWithSpaces));

		// Set original string too, since the big name is created using original
		// strings (if possible) -- err, no, we really need the original
		// string to be the true original string.
		// String bigNameWithSpaces = bigName.replace('_', ' ');
		// rightNode.set("orig_str", new FeatureNode(bigNameWithSpaces));
		rightNode.set("collocation_end", rightNode);
		rightNode.set("collocation_start", leftNode);
		rightNode.set("start_char", leftNode.get("start_char"));

		// Use morphology on the right node, just in case.
		// ??? Really ??? the combined thing will have underscores ... 
		// Does wordnet really have morphology for these things?
		// Shouldn't we have done morphology earlier, and not now?
		// Yeah, I don't think so. This just doesn't make sense.
		// FeatureNode orig = rightNode.get("str");
		// MorphyAlg m = new MorphyAlg();
		// m.applyTo(rightNode, context);
		// rightNode.set("str", orig);
	}
}
