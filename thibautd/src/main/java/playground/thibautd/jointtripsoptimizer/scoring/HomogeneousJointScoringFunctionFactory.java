/* *********************************************************************** *
 * project: org.matsim.*
 * HomogeneousJointScoringFunctionFactory.java
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
package playground.thibautd.jointtripsoptimizer.scoring;

import org.matsim.api.core.v01.population.Plan;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * @author thibautd
 */
public class HomogeneousJointScoringFunctionFactory implements ScoringFunctionFactory {
	private final PlanCalcScoreConfigGroup config;

	public HomogeneousJointScoringFunctionFactory(final PlanCalcScoreConfigGroup config) {
		this.config = config;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(
				new HomogeneousJointActivityScoring(plan, this.config));
		scoringFunctionAccumulator.addScoringFunction(
				new HomogeneousJointLegScoring(plan, this.config));

		return scoringFunctionAccumulator;
	}
}

