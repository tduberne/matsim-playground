/* *********************************************************************** *
 * project: org.matsim.*
 * LinkTopology.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.analysis.possiblesharedrides;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;

/**
 * Defines a topology on links.
 * A link (a,b) is neighbor of (c,d) if min[ d(x,y) ] &lt; dmax,
 * where d is the euclidean distance, x &isin; {a,b}, y &isin; {c,d},
 * dmax is an "acceptable distance".
 *
 * @author thibautd
 */
public class LinkTopology {

	private final QuadTree<Id> quadTree =
		new QuadTree<Id>(
				Double.NEGATIVE_INFINITY,
				Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY,
				Double.POSITIVE_INFINITY);
	private double acceptableDistance;
	private final Map<Id, ? extends Link> network;
	private final Map<Id, List<Id>> neighborhoods = new HashMap<Id, List<Id>>();

	public LinkTopology(
			final Network network,
			final double acceptableDistance) {
		this.acceptableDistance = acceptableDistance;
		this.network = network.getLinks();

		Coord fromNode, toNode;

		for (Map.Entry<Id, ? extends Link> link :
				network.getLinks().entrySet()) {
			fromNode = link.getValue().getFromNode().getCoord();
			toNode = link.getValue().getToNode().getCoord();

			// add link in the octree at th location of both nodes
			this.quadTree.put(fromNode.getX(), fromNode.getY(), link.getKey());
			this.quadTree.put(toNode.getX(), toNode.getY(), link.getKey());
		}
	}

	public List<Id> getNeighbors(final Id linkId) {
		List<Id> output = this.neighborhoods.get(linkId);

		if (output == null) {
			output = new ArrayList<Id>();
			Link link = this.network.get(linkId);
			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();

			output.addAll( this.quadTree.get(
						from.getX(),
						from.getY(),
						this.acceptableDistance));
			output.addAll( this.quadTree.get(
						to.getX(),
						to.getY(),
						this.acceptableDistance));
		}

		return output;
	}
}

