/* *********************************************************************** *
 * project: org.matsim.*
 * RestrictedTournamentSelector.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jgap.Configuration;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.NaturalSelectorExt;
import org.jgap.Population;
import org.jgap.RandomGenerator;

import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Selector using a "restricted tournament" (cf Harik 1995) to generate the new 
 * population.
 * The idea is the following:
 * <ul>
 *  <li>for each "new born", <i>w</i> chromosomes from the population are selected
 *   randomly.
 *  <li>the "closest" chromosome of this set is selected for competition
 *  <li>if the new born is fittest than the chromosome competitor, it replaces it
 *   (and becomes thus part of the possible competitors for not yet added new
 *   born)
 * </ul>
 *
 * @author thibautd
 */
public class RestrictedTournamentSelector extends NaturalSelectorExt {

	private static final long serialVersionUID = 1L;

	private final List<IChromosome> newBorned = new ArrayList<IChromosome>();
	// protected for use in the "tabued" version. If approach works, improve.
	protected final List<IChromosome> agedIndividuals = new ArrayList<IChromosome>();
	private final int windowSize;
	private final Configuration jgapConfig;
	private final ChromosomeDistanceComparator distanceComparator;
	private final Random random;

	/**
	 * @param jgapConfig the config used in the optimisation. The sample Chomosome
	 * and the population size MUST be initialized.
	 */
	public RestrictedTournamentSelector(
			final Configuration jgapConfig,
			final JointReplanningConfigGroup configGroup,
			final ChromosomeDistanceComparator distanceComparator
			) throws InvalidConfigurationException {
		super(jgapConfig);
		//this.windowSize = Math.min(
		//		configGroup.getRtsWindowSize(),
		//		//jgapConfig.getPopulationSize());
		//		configGroup.getPopulationSize());
		int paramWindowSize = (int) Math.ceil(
				configGroup.getWindowSizeIntercept() +
				configGroup.getWindowSizeCoef() * jgapConfig.getPopulationSize());
		paramWindowSize = Math.min(
				paramWindowSize,
				jgapConfig.getPopulationSize());

		this.windowSize = Math.max(1, paramWindowSize);

		this.jgapConfig = jgapConfig;
		this.random = new Random( jgapConfig.getRandomGenerator().nextLong() + 194534 );
		this.distanceComparator = distanceComparator;
	}

	@Override
	public void empty() {
		this.newBorned.clear();
		this.agedIndividuals.clear();
	}

	@Override
	public boolean returnsUniqueChromosomes() {
		return true;
	}

	@Override
	protected void add(
			final IChromosome chromosome) {
		if (chromosome.getAge() == 0) {
			this.newBorned.add(chromosome);
		}
		else {
			this.agedIndividuals.add(chromosome);
		}
	}

	/**
	 * Selects the chromosomes for the next generation.
	 */
	@Override
	protected void selectChromosomes(
			final int nToSelect,
			final Population nextGeneration) {
		List<IChromosome> window;
		IChromosome closestOldCompetitor;

		// examine all new borned and make them compete with old fellows by RTS
		for (IChromosome competitor : this.newBorned) {
			window = getWindow();
			this.distanceComparator.setComparisonData(competitor, window);
			//Collections.sort(window, this.distanceComparator);
			//closestOldCompetitor = window.get(0);
			closestOldCompetitor = Collections.min(window, this.distanceComparator);

			if (competitor.getFitnessValue() > closestOldCompetitor.getFitnessValue()) {
				this.agedIndividuals.add(competitor);
				this.agedIndividuals.remove(closestOldCompetitor);
			}
		}

		for (IChromosome chrom : this.agedIndividuals) {
			nextGeneration.addChromosome(chrom);
		}

		if (nextGeneration.size() != nToSelect) {
			throw new IllegalArgumentException("RTS must be used to generate the"
					+" full population: toSelect="+nToSelect+", generationSize="+
					nextGeneration.size());
		}
	}

	private List<IChromosome> getWindow() {
		//List<IChromosome> window = new ArrayList<IChromosome>(this.windowSize);
		//int index;
		//List<Integer> selected = new ArrayList<Integer>(this.windowSize);
		// RandomGenerator generator =  this.jgapConfig.getRandomGenerator();
		Collections.shuffle( agedIndividuals , random );
		
		//for (int i=0; i < this.windowSize; i++) {
		//	do {
		//		index = generator.nextInt(this.windowSize);
		//	} while (selected.contains(index));

		//	selected.add(index);
		//	window.add(this.agedIndividuals.get(index));
		//	window.add(this.agedIndividuals.get(index));
		//}

		return this.agedIndividuals.subList(0, windowSize);
	}
}

