/*
 * Copyright 2008 Novamente LLC
 * Copyright 2009 Linas Vepstas
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

import java.io.Serializable;
import java.util.List;

public abstract class EntityTagger
{
	// An array of EntityInfos, ordered by their order in the sentence
	private List<EntityInfo> orderedEntityInfos;

	public abstract List<EntityInfo> tagEntities(String sentence);

	// --------------------------------------------------------

	/**
	 * Add the entity info to the list, inserting it in sorted order.
	 */
	public void addEntity(EntityInfo ei)
	{
		if (null == orderedEntityInfos)
			orderedEntityInfos = new ArrayList<EntityInfo>();

		int open = 0;
		int start = ei.getFirstCharIndex();
		int end = ei.getLastCharIndex();
		for (EntityInfo e: orderedEntityInfos)
		{
			int beg = e.getFirstCharIndex();
			if ((open <= start) && (end <= beg))
			{
				int idx = orderedEntityInfos.indexOf(e);
				orderedEntityInfos.add(idx, ei);
				return;
			}
			open = e.getLastCharIndex();

			// If our entity overlaps with existing entities, ignore it.
			if (start < open) return;
		}
		orderedEntityInfos.add(ei);
	}

	// --------------------------------------------------------

	/**
	 * Return all EntityInfo's ordered by their starting character
	 * position.
	 */
	public List<EntityInfo> getEntities()
	{
		return orderedEntityInfos;
	}

	public boolean equals(Object other)
	{
		if (! (other instanceof EntityTagger)) return false;
		EntityTagger et = (EntityTagger)other;
		return this.orderedEntityInfos.equals(et.orderedEntityInfos);
	}
}
