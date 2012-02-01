/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizer.java
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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer;

import java.util.Random;

import org.jgap.Genotype;
import org.jgap.InvalidConfigurationException;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.replanning.JointPlanAlgorithm;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;

/**
 * {@link PlanAlgorithm} aimed at optimizing joint plans with a genetic algorithm.
 * @author thibautd
 */
public class JointPlanOptimizer extends JointPlanAlgorithm {
	private final JointReplanningConfigGroup configGroup;
	private final String outputPath;
	private final JointPlanOptimizerSemanticsBuilder semanticsBuilder;

	private final Random randomGenerator = MatsimRandom.getLocalInstance();

	public JointPlanOptimizer(
			final JointReplanningConfigGroup configGroup,
			final JointPlanOptimizerSemanticsBuilder semanticsBuilder,
			final String iterationOutputPath
			) {
		this.configGroup = configGroup;
		this.semanticsBuilder = semanticsBuilder;
		this.outputPath = iterationOutputPath;
	}

	/**
	 * the actual optimisation algorithm, operating on a joint plan.
	 */
	@Override
	public void run(final JointPlan plan) {
		if (!isOptimizablePlan(plan)) {
			return;
		}

		JointPlanOptimizerJGAPConfiguration jgapConfig =
			new JointPlanOptimizerJGAPConfiguration(
					plan,
					configGroup,
					semanticsBuilder,
					outputPath,
					randomGenerator.nextLong());

		Genotype gaPopulation;
		try {
			gaPopulation = Genotype.randomInitialGenotype( jgapConfig );
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException( e );
		}

		if (this.configGroup.getFitnessToMonitor()) {
			//log.debug("monitoring fitness");
			gaPopulation.evolve(jgapConfig.getEvolutionMonitor());
		}
		else {
			gaPopulation.evolve(this.configGroup.getMaxIterations());
		}

		// notify end
		jgapConfig.finish();

		//get fittest chromosome, and modify the given plan accordingly
		JointPlan evolvedPlan = jgapConfig.getDecoder().decode(
				gaPopulation.getFittestChromosome());
				//((JointPlanOptimizerJGAPBreeder) jgapConfig.getBreeder()).getAllTimesBest());
		plan.resetFromPlan(evolvedPlan);
		plan.resetScores();
	}

	private boolean isOptimizablePlan(final JointPlan plan) {
		for (Plan indivPlan : plan.getIndividualPlans().values()) {
			if (indivPlan.getPlanElements().size() > 1) {
				return true;
			}
		}
		return false;
	}
}

