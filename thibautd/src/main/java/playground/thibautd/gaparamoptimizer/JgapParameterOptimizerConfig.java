/* *********************************************************************** *
 * project: org.matsim.*
 * JgapParameterOptimizerConfig.java
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
package playground.thibautd.gaparamoptimizer;

import java.util.List;

import org.apache.log4j.Logger;
import org.jgap.Configuration;
import org.jgap.DefaultFitnessEvaluator;
import org.jgap.InvalidConfigurationException;
import org.jgap.event.EventManager;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.MutationOperator;
import org.jgap.impl.StockRandomGenerator;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;

/**
 * A simple jgap config to optimize the parameters of the JPO
 * @author thibautd
 */
public class JgapParameterOptimizerConfig extends Configuration {
	private static final Logger log =
		Logger.getLogger(JgapParameterOptimizerConfig.class);

	private static final long serialVersionUID = 1L;

	private static final int POP_SIZE = 20;
	private static final int MUTATION_RATE = 10;
	private static final double CO_RATE = 0.6d;
	private static final double ORIG_RATE = 0.8d;
	private static final boolean DOUBLETTES = false;
	private static final long SEED = 1984;

	/**
	 * @param plans the JointPlans to use as test instances
	 * @param scoringFunctionFactory parameter of the replanning algorithm
	 * @param legTravelTimeEstimatorFactory parameter of the replanning algorithm
	 * @param routingAlgorithm parameter of the replanning algorithm
	 * @param network parameter of the replanning algorithm
	 * @param iterationOutputPath parameter of the replanning algorithm
	 */
	public JgapParameterOptimizerConfig(
			final List<JointPlan> plans,
			final ScoringFunctionFactory scoringFunctionFactory,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			// final PlansCalcRoute routingAlgorithm,
			final Controler controler,
			final Network network,
			final String iterationOutputPath
			) {
		super(null);
		log.debug("initializing jgap conf...");
		log.debug("population size: "+POP_SIZE);
		log.debug("p_mut: "+1d/MUTATION_RATE);
		log.debug("co_rate: "+CO_RATE);
		log.debug("orig rate: "+ORIG_RATE);
		Configuration.reset();
		ParameterOptimizerFitness fitness = new ParameterOptimizerFitness(
				this, plans, scoringFunctionFactory, legTravelTimeEstimatorFactory,
				controler, network, iterationOutputPath);

		try {
			// default JGAP objects initializations
			this.setBreeder(new JgapBreeder( controler.getConfig().global().getNumberOfThreads() ));
			this.setEventManager(new EventManager());

			// seed the default JGAP pseudo-random generator with a matsim random
			// number, so that the simulations are reproducible.
			this.setRandomGenerator( new StockRandomGenerator() );
			((StockRandomGenerator) this.getRandomGenerator()).setSeed( SEED );

			// selector
			BestChromosomesSelector selector = new BestChromosomesSelector(this, ORIG_RATE);
			//NaturalSelector selector = new WeightedRouletteSelector(this);
			selector.setDoubletteChromosomesAllowed( DOUBLETTES );
			this.addNaturalSelector( selector , false );

			//this.setPreservFittestIndividual( false );

			this.setSampleChromosome(fitness.getSampleChromosome());

			this.setPopulationSize(POP_SIZE);

			this.setFitnessEvaluator(new DefaultFitnessEvaluator());
			this.setFitnessFunction(fitness);

			// discarded chromosomes are "recycled" rather than suppressed.
			this.setChromosomePool(new ChromosomePool());

			// genetic operators definitions
			this.addGeneticOperator( new CrossoverOperator(this, CO_RATE));
			this.addGeneticOperator( new MutationOperator(this, MUTATION_RATE));

		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}

		log.debug("initializing jgap conf... DONE");
	}
}

