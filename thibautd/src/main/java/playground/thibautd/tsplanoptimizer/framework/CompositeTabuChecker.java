/* *********************************************************************** *
 * project: org.matsim.*
 * CompositeTabuChecker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.tsplanoptimizer.framework;

import java.util.ArrayList;
import java.util.List;

/**
 * A tabu checker aggregating several ones.
 * A move is tabu if it is tabu for a least one registered TabuChecker.
 * @author thibautd
 */
public class CompositeTabuChecker implements TabuChecker {
	private final List<TabuChecker> checkers = new ArrayList<TabuChecker>();

	@Override
	public void notifyMove(
			final Solution solution,
			final Move move,
			final double score) {
		for (TabuChecker checker : checkers) {
			checker.notifyMove( solution , move , score );
		}
	}

	@Override
	public boolean isTabu(
			final Solution solution,
			final Move move) {
		for (TabuChecker checker : checkers) {
			if (checker.isTabu( solution , move )) {
				return true;
			}
		}
		return false;
	}

	public void add(final TabuChecker checker) {
		checkers.add( checker );
	}
}

