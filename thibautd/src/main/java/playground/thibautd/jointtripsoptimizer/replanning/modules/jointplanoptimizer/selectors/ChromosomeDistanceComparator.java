/* *********************************************************************** *
 * project: org.matsim.*
 * ChromosomeDistanceComparator.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.jointplanoptimizer.selectors;

import java.util.Comparator;
import java.util.List;

import org.jgap.IChromosome;

/**
 * Comparator aimed at sorting chromosomes according to their distance to a given
 * chromosome.
 * @author thibautd
 */
public abstract class ChromosomeDistanceComparator implements Comparator<IChromosome> {

	//private final Map<IChromosome, Double> distanceMap = 
	//	new HashMap<IChromosome, Double>();
	private IChromosome newBorn;

	public void setComparisonData(
			final IChromosome newBorn, 
			final List<IChromosome> window) {
		//distanceMap.clear();

		//for (IChromosome chrom : window) {
		//	distanceMap.put(chrom, getDistance(newBorn, chrom));
		//}
		this.newBorn = newBorn;
	}

	/**
	 * A chromosome is "greater" than another if it is closer to the new borned.
	 * This method computes distances on the fly.
	 */
	@Override
	public int compare(
			final IChromosome chr1,
			final IChromosome chr2) {
		double d1, d2;

		if (chr1.equals(chr2)) {
			return 0;
		}

		d1 = getDistance(newBorn, chr1);
		d2 = getDistance(newBorn, chr2);

		return d1 == d2 ? 0 : (d1 > d2 ? 1 : -1);
	}

	/**
	 * Defines the distance.
	 */
	protected abstract double getDistance(IChromosome chr1, IChromosome chr2);
}

