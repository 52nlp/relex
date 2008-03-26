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

import relex.feature.FeatureNode;

/**
 * Holder of phrase chunks
 *
 * Copyright (C) 2008 Linas Vepstas <linas@linas.org>
 */

public class Chunk
{
	private ArrayList<FeatureNode> chunk;
	public Chunk()
	{
		chunk = new ArrayList<FeatureNode>();
	}

	public void addWord(FeatureNode fn)
	{
		chunk.add(fn);
	}
	public void addWords(ArrayList<FeatureNode> words)
	{
		chunk.addAll(words);
	}
	public void clear()
	{
		chunk.clear();
	}

	public String toString()
	{
		String str = "";
		for (int i=0; i<chunk.size(); i++)
		{
			FeatureNode fn = chunk.get(i);
			fn = fn.get("str");
			if (fn != null)
			{
				str += fn.getValue();
				str += " ";
			}
		}
		return str;
	}
}
