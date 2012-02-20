/* *********************************************************************** *
 * project: org.matsim.*
 * TabuChecker.java
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

/**
 * @author thibautd
 */
public interface TabuChecker extends AppliedMoveListener {
	/**
	 * Says if a move is tabu.
	 * @param solution the solution before the move
	 * @param move the to apply
	 * @return true if the move is tabu, false otherwise
	 */
	public boolean isTabu( Solution solution , Move move );
}

