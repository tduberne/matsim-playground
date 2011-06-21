/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractJointPlanOptimizerFitnessFunction.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.fitness;

import org.jgap.FitnessFunction;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerDecoder;

/**
 * Extend this class to provide a fitness function for the joint replanning algorithm.
 *
 *
 * @author thibautd
 */
abstract public class AbstractJointPlanOptimizerFitnessFunction extends FitnessFunction {
	/**
	 * @return a decoder which creates plans consistent with the scores
	 */
	abstract public JointPlanOptimizerDecoder getDecoder(); 
}

