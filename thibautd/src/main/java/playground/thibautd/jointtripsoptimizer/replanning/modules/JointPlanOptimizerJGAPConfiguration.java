/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerJGAPConfiguration.java
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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.jgap.Configuration;
import org.jgap.DefaultFitnessEvaluator;
import org.jgap.event.EventManager;
import org.jgap.Gene;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.StockRandomGenerator;
import org.jgap.impl.ThresholdSelector;
import org.jgap.InvalidConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;

import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.JointActivity;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * JGAP configuration object for the joint replanning algorithm.
 *
 * @todo improve substantially:
 *   - correct the number of "toggle" chromosomes
 *   - include a "engagement/desengagement" functionality
 *   - make possible the joint trips where several passengers have different O/D
 * @author thibautd
 */
public class JointPlanOptimizerJGAPConfiguration extends Configuration {

	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPConfiguration.class);

	private static final Double DAY_DUR = 24*3600d;

	private static final long serialVersionUID = 1L;

	//TODO: make final
	private int numEpisodes;
	private int numToggleGenes;
	/**
	 * stores the number of duration genes relatives to each individual plan in
	 * the joint plan.
	 */
	private final List<Integer> nDurationGenes = new ArrayList<Integer>();
	private JointPlanOptimizerFitnessFunction fitnessFunction;

	private final boolean optimizeToggle;

	public JointPlanOptimizerJGAPConfiguration(
			JointPlan plan,
			JointReplanningConfigGroup configGroup,
			ScoringFunctionFactory scoringFunctionFactory,
			LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			PlansCalcRoute routingAlgorithm,
			Network network,
			String outputPath,
			long randomSeed
			) {
		super(null);
		Configuration.reset();

		this.optimizeToggle = configGroup.getOptimizeToggle();

		if (this.optimizeToggle) {
			throw new UnsupportedOperationException("toggle optimization possibly"
					+" broken: temporarily unsupported.");
		}
		// get info on the plan structure
		this.countEpisodes(plan);

		try {
			// default JGAP objects initializations
			this.setBreeder(new JointPlanOptimizerJGAPBreeder());
			this.setEventManager(new EventManager());

			// seed the default JGAP pseudo-random generator with a matsim random
			// number, so that the simulations are reproducible.
			this.setRandomGenerator(new StockRandomGenerator());
			((StockRandomGenerator) this.getRandomGenerator()).setSeed(randomSeed);

			// elitism: unnecessary with BestChromosomesSelector or ThresholdSelector
			this.setPreservFittestIndividual(true);


			// select the best chromosomes
			// BestChromosomesSelector selector = new BestChromosomesSelector(this);
			// selector.setDoubletteChromosomesAllowed(false);

			//random choice based on the fitness values
			//XXX unsatisfied dependencies
			//WeightedRouletteSelector selector = new WeightedRouletteSelector();

			//random selectors ensurring that a certain proportion of th selected
			//chromosomes are among the bests, the others beeing selected randomly
			ThresholdSelector selector =
				new ThresholdSelector(this, configGroup.getSelectionThreshold());
			this.addNaturalSelector(selector, false);

			// Chromosome: construction
			Gene[] sampleGenes = new Gene[this.numToggleGenes + this.numEpisodes];
			for (int i=0; i < this.numToggleGenes; i++) {
				sampleGenes[i] = new BooleanGene(this);
			}
			for (int i=this.numToggleGenes;
					i < this.numToggleGenes + this.numEpisodes; i++) {
				//sampleGenes[i] = new IntegerGene(this, 0, numTimeIntervals);
				sampleGenes[i] = new DoubleGene(this, 0, DAY_DUR);
			}

			log.debug("duration genes: "+this.numEpisodes+", toggle chromosomes: "+this.numToggleGenes);
			this.setSampleChromosome(new JointPlanOptimizerJGAPChromosome(this, sampleGenes));

			// population size
			log.debug("population size set to "+configGroup.getPopulationSize());
			this.setPopulationSize(configGroup.getPopulationSize());

			this.fitnessFunction = new JointPlanOptimizerFitnessFunction(
						plan,
						configGroup,
						legTravelTimeEstimatorFactory,
						routingAlgorithm,
						network,
						this.numToggleGenes,
						this.numEpisodes,
						scoringFunctionFactory);
			this.setFitnessEvaluator(new DefaultFitnessEvaluator());
			this.setFitnessFunction(this.fitnessFunction);

			// discarded chromosomes are "recycled" rather than suppressed.
			this.setChromosomePool(new ChromosomePool());

			// genetic operators definitions
			if (configGroup.getPlotFitness()) {
				this.addGeneticOperator( new JointPlanOptimizerPopulationAnalysisOperator(
							this,
							configGroup.getMaxIterations(),
							outputPath));
			}
			this.addGeneticOperator( new JointPlanOptimizerJGAPCrossOver(
						this,
						configGroup,
						this.numToggleGenes,
						this.numEpisodes,
						this.nDurationGenes) );
			this.addGeneticOperator( new JointPlanOptimizerJGAPMutation(
						this,
						configGroup,
						this.numToggleGenes + this.numEpisodes,
						this.nDurationGenes));

		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e.getMessage());
		}
	 }

	/**
	 * Sets the private variables numEpisodes and numToggleGenes.
	 * an episode corresponds to an activity and its eventual access trip.
	 * a joint episode is an episode which involves a joint trip.
	 */
	//TODO: make toggle gene number consistent with the possibility of
	//several passengers with several O/D.
	//Done, but to check
	private void countEpisodes(JointPlan plan) {
		Id[] ids = new Id[1];
		ids = plan.getClique().getMembers().keySet().toArray(ids);
		//List<JointLeg> alreadyExamined = new ArrayList<JointLeg>();
		List<PlanElement> currentPlan;
		int currentNDurationGenes;
		boolean sharedRideExamined = false;

		this.numEpisodes = 0;
		this.numToggleGenes = 0;
		this.nDurationGenes.clear();

		for (Id id : ids) {
			currentPlan = plan.getIndividualPlan(id).getPlanElements();
			currentNDurationGenes = 0;
			//TODO: use indices (and suppress the booleans)
			for (PlanElement pe : currentPlan) {
				// count activities for which duration is optimized
				if ((pe instanceof JointActivity)&&
						(!((JointActivity) pe).getType().equals(JointActingTypes.PICK_UP))&&
						(!((JointActivity) pe).getType().equals(JointActingTypes.DROP_OFF)) ) {
					currentNDurationGenes++;

					if (sharedRideExamined) {
						//reset the marker
						sharedRideExamined = false;
					}
				} else if ((this.optimizeToggle)&&
						(pe instanceof JointLeg)&&
						(((JointLeg) pe).getJoint())&&
						//(!alreadyExamined.contains(pe))
						(((JointLeg) pe).getMode().equals(TransportMode.car))&&
						(!sharedRideExamined)
						) {
					// we are on the first shared ride of a passenger ride
					this.numToggleGenes++;
					sharedRideExamined = true;

					//alreadyExamined.addAll(
					//		((JointLeg) pe).getLinkedElements().values());
				}
			}
			//do not count last activity
			currentNDurationGenes--;
			this.numEpisodes += currentNDurationGenes;
			this.nDurationGenes.add(currentNDurationGenes);
		 }
	 }

	public JointPlanOptimizerDecoder getDecoder() {
		return this.fitnessFunction.getDecoder();
	}

	public int getNumEpisodes() {
		return this.numEpisodes;
	}

	public int getNumJointEpisodes() {
		return this.numToggleGenes;
	}

	/**
	 * to avoid multiplying the places where the day duration is defined.
	 * Not very elegant, should be moved somewhere else, for example in the config
	 * group.
	 */
	public double getDayDuration() {
		return this.numToggleGenes;
	}
}

