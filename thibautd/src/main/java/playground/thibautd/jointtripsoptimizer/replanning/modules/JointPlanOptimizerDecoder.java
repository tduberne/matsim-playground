/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerDecoder.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import org.jgap.IChromosome;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;

/**
 * Transforms a genotype into a JointPlan.
 * @author thibautd
 */
public interface JointPlanOptimizerDecoder {
	public JointPlan decode(IChromosome chromosome);
}
