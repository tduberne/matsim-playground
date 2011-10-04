/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerOTFFitnessFunction.java
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

import org.jgap.IChromosome;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerDecoder;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.DurationOnTheFlyScorer;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.JointPlanOptimizerDecoderFactory;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.JointPlanOptimizerPartialDecoderFactory;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Implementation of a scoring function that scores durations "on the fly" rather
 * than on a decoded JointPlan instance.
 *
 * @author thibautd
 */
public class JointPlanOptimizerOTFFitnessFunction extends AbstractJointPlanOptimizerFitnessFunction {

	private static final long serialVersionUID = 1L;

	private final JointPlanOptimizerDecoder decoder;
	private final JointPlanOptimizerDecoder fullDecoder;
	private final DurationOnTheFlyScorer scorer;
	private IChromosome lastIndividuallyScored = null;;
	private JointPlan lastResultingPlan = null;

	public JointPlanOptimizerOTFFitnessFunction(
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			final PlansCalcRoute routingAlgorithm,
			final Network network,
			final int numJointEpisodes,
			final int numEpisodes,
			final int nMembers,
			final ScoringFunctionFactory scoringFunctionFactory) {
		super();
		this.decoder = (new JointPlanOptimizerPartialDecoderFactory(
					plan,
					configGroup,
					numJointEpisodes,
					numEpisodes)).createDecoder();
		this.fullDecoder = (new JointPlanOptimizerDecoderFactory(
					plan,
					configGroup,
					legTravelTimeEstimatorFactory,
					routingAlgorithm,
					network,
					numJointEpisodes,
					numEpisodes,
					nMembers)).createDecoder();
		this.scorer = new DurationOnTheFlyScorer(
					plan,
					configGroup,
					scoringFunctionFactory,
					legTravelTimeEstimatorFactory,
					routingAlgorithm,
					network,
					numJointEpisodes,
					numEpisodes,
					nMembers);
	}

	@Override
	protected double evaluate(final IChromosome chromosome) {
		if (lastIndividuallyScored == chromosome) {
			return lastResultingPlan.getScore();
		}

		JointPlan plan = this.decoder.decode(chromosome);
		return this.scorer.score(chromosome, plan);
	}

	public JointPlanOptimizerDecoder getDecoder() {
		return this.fullDecoder;
	}

	@Override
	protected double[] evaluateIndividuals(IChromosome chromosome) {
		lastIndividuallyScored = chromosome;
		lastResultingPlan = this.decoder.decode(chromosome);
		return this.scorer.scoreSeparately(chromosome, lastResultingPlan);
	}
}

