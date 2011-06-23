/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerJGAPCrossOver.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.geneticoperators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import org.jgap.Gene;
import org.jgap.GeneticOperator;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;
import org.jgap.Population;
import org.jgap.RandomGenerator;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPChromosome;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Cross breeds joint plans.
 * It does the following:
 * <ul>
 * <li> on discrete variables: uniform cross-over.
 * <li> on continuous variables: GENOCOP-like "arithmetical" cross overs.
 * </ul>
 *
 * assumes the following structure for the chromosome: [boolean genes]-[Double genes]-[mode genes]
 * @author thibautd
 */
public class JointPlanOptimizerJGAPCrossOver implements GeneticOperator {
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPCrossOver.class);

	private static final long serialVersionUID = 1L;

	private static final double EPSILON = 1e-10;

	private final double WHOLE_CO_RATE;
	private final double SIMPLE_CO_RATE;
	private final double SINGLE_CO_RATE;
	private final int N_BOOL;
	private final int N_DOUBLE;
	private final int N_MODE;

	private final List<Integer> nDurationGenes = new ArrayList<Integer>();

	private final double DAY_DURATION;

	private final RandomGenerator randomGenerator;

	/**
	 * Constructor for use with static rates
	 */
	public JointPlanOptimizerJGAPCrossOver(
			final JointPlanOptimizerJGAPConfiguration config,
			final JointReplanningConfigGroup configGroup,
			final int numBooleanGenes,
			final int numDoubleGenes,
			final int numModeGenes,
			final List<Integer> nDurationGenes
			) {
		this.WHOLE_CO_RATE = configGroup.getWholeCrossOverProbability();
		this.SIMPLE_CO_RATE = configGroup.getSimpleCrossOverProbability();
		this.SINGLE_CO_RATE = configGroup.getSingleCrossOverProbability();
		this.N_BOOL = numBooleanGenes;
		this.N_DOUBLE = numDoubleGenes;
		this.N_MODE = numModeGenes;
		this.DAY_DURATION = config.getDayDuration();
		this.nDurationGenes.clear();
		this.nDurationGenes.addAll(nDurationGenes);
		this.randomGenerator = config.getRandomGenerator();
	}

	@Override
	public void operate(
			final Population a_population,
			final List a_candidateChromosome
			) {
		int populationSize = a_population.size();
		int numOfWholeCo;
		int numOfSimpleCo;
		int numOfSingleCo;

		numOfWholeCo = getNumberOfOperations(this.WHOLE_CO_RATE, populationSize);
		numOfSimpleCo = getNumberOfOperations(this.SIMPLE_CO_RATE, populationSize);
		numOfSingleCo = getNumberOfOperations(this.SINGLE_CO_RATE, populationSize);

		int numOfCo = numOfWholeCo + numOfSimpleCo + numOfSingleCo;
		int index1;
		IChromosome parent1;
		IChromosome mate1;
		int index2;
		IChromosome parent2;
		IChromosome mate2;

		for (int i=0; i < numOfCo; i++) {
			// draw random parents
			index1 = this.randomGenerator.nextInt(populationSize);
			index2 = this.randomGenerator.nextInt(populationSize);
			parent1 = (IChromosome) a_population.getChromosome(index1);
			parent2 = (IChromosome) a_population.getChromosome(index2);
			mate1 = (IChromosome) parent1.clone();
			mate2 = (IChromosome) parent2.clone();

			//doBooleanCrossOver(mate1, mate2);
			//doModeCrossOver(mate1, mate2);
			if (i < numOfWholeCo) {
				doDoubleWholeCrossOver(mate1, mate2);
			}
			else if (i < numOfWholeCo + numOfSimpleCo) {
				doDoubleSimpleCrossOver(mate1, mate2);
			}
			else {
				doDoubleSingleCrossOver(mate1, mate2);
			}

			a_candidateChromosome.add(mate1);
			a_candidateChromosome.add(mate2);
		}
	}

	private int getNumberOfOperations(final double rate, final double populationSize) {
		// always perform at least one operation of each CO
		//return Math.max(1, (int) Math.ceil(rate * populationSize));
		return (int) Math.ceil(rate * populationSize);
	}

	/**
	 * Performs a uniform cross-over on the boolean valued genes.
	 */
	private void doBooleanCrossOver(
			final IChromosome mate1,
			final IChromosome mate2) {
		boolean value1;
		boolean value2;
		// loop over boolean genes
		for (int i=0; i < this.N_BOOL; i++) {
			value1 = ((BooleanGene) mate1.getGene(i)).booleanValue();
			value2 = ((BooleanGene) mate2.getGene(i)).booleanValue();

			// exchange values with proba O.5
			if (this.randomGenerator.nextInt(2) == 0) {
				mate1.getGene(i).setAllele(value2);
				mate2.getGene(i).setAllele(value1);
			}
		}
	}

	/**
	 * Performs a uniform cross-over on the mode genes.
	 */
	private void doModeCrossOver(
			final IChromosome mate1,
			final IChromosome mate2) {
		Object value1;
		Object value2;

		// loop over boolean genes
		for (int i=this.N_BOOL + this.N_DOUBLE;
				i < this.N_BOOL + this.N_DOUBLE + this.N_MODE;
				i++) {
			value1 = mate1.getGene(i).getAllele();
			value2 = mate2.getGene(i).getAllele();

			// exchange values with proba O.5
			if (this.randomGenerator.nextInt(2) == 0) {
				mate1.getGene(i).setAllele(value2);
				mate2.getGene(i).setAllele(value1);
			}
		}
	}


	/**
	 * Performs a "GENOCOP-like" "Whole arithmetical cross-over" on the double
	 * valued genes, with a random coefficient.
	 */
	private void doDoubleWholeCrossOver(
			final IChromosome mate1,
			final IChromosome mate2) {
		DoubleGene gene1;
		DoubleGene gene2;
		double oldValue1;
		double oldValue2;
		double randomCoef = this.randomGenerator.nextDouble();

		for (int i=this.N_BOOL; i < this.N_BOOL + this.N_DOUBLE; i++) {
			gene1 = (DoubleGene) mate1.getGene(i);
			gene2 = (DoubleGene) mate2.getGene(i);
			oldValue1 = gene1.doubleValue();
			oldValue2 = gene2.doubleValue();
			
			gene1.setAllele(randomCoef*oldValue1 + (1 - randomCoef)*oldValue2);
			gene2.setAllele(randomCoef*oldValue2 + (1 - randomCoef)*oldValue1);
		}
	}

	/**
	 * Performs a "GENOCOP-like" "Simple arithmetical cross-over" on the double
	 * valued genes.
	 */
	private void doDoubleSimpleCrossOver(
			final IChromosome mate1,
			final IChromosome mate2) {
		DoubleGene gene1;
		DoubleGene gene2;
		double oldValue1;
		double oldValue2;
		int crossingPoint = this.randomGenerator.nextInt(this.N_DOUBLE);
		double crossingCoef1 = simpleCoCrossingCoef(
				mate1.getGenes(),
				mate2.getGenes(),
				crossingPoint);
		double crossingCoef2 = simpleCoCrossingCoef(
				mate2.getGenes(),
				mate1.getGenes(),
				crossingPoint);
		//only compute "useful" values: more efficient and avoids cumulating
		//rouding errors
		boolean cross1 = (crossingCoef1 > EPSILON);
		boolean cross2 = (crossingCoef2 > EPSILON);

		if ( cross1 || cross2 ) {
			for (int i=this.N_BOOL + crossingPoint; i < this.N_BOOL + this.N_DOUBLE; i++) {
				gene1 = (DoubleGene) mate1.getGene(i);
				gene2 = (DoubleGene) mate2.getGene(i);
				oldValue1 = gene1.doubleValue();
				oldValue2 = gene2.doubleValue();
				
				if (cross1) {
					gene1.setAllele(
							(1d - crossingCoef1)*oldValue1 +
							crossingCoef1*oldValue2);
				}
				if (cross2) {
					gene2.setAllele(
							crossingCoef2*oldValue1 +
							(1d - crossingCoef2)*oldValue2);
				}
			}
		}
	}

	/**
	 * computes the "crossing coefficient" for a simple arithmetic CO.
	 * This corresponds to the largest a such that the vector 
	 * (x_1, x_2, ..., x_(i-1), (1 - a)x_i + a*y_i, ..., (1 - a)x_n + a*y_n)
	 * respects the constraints. A value of one corresponds to a classical single
	 * point cross-over.
	 */
	private double simpleCoCrossingCoef(
			final Gene[] mate1Genes,
			final Gene[] mate2Genes,
			final int crossingPoint) {
		double mate1PlanDuration = 0d;
		double crossOverSurplus = 0d;
		double currentSurplus;
		double currentEpisodeDuration1;
		double currentEpisodeDuration2;
		double minIndivCoef = Double.POSITIVE_INFINITY;
		Iterator<Integer> nGenesIterator = this.nDurationGenes.iterator();
		int currentNGenes=nGenesIterator.next();
		int countGenes = 0;
		//double minPosDurationCoef = Double.POSITIVE_INFINITY;

		// move count gene through the uncrossed part of the plan and
		// initialize the first plan duration.
		for (int i=this.N_BOOL; i < this.N_BOOL + crossingPoint; i++) {
			if (countGenes == currentNGenes) {
				countGenes = 1;
				currentNGenes = nGenesIterator.next();
				mate1PlanDuration = ((DoubleGene) mate1Genes[i]).doubleValue();
			} else {
				mate1PlanDuration += ((DoubleGene) mate1Genes[i]).doubleValue();
				countGenes++;
			}
		}
	
		for (int i=this.N_BOOL + crossingPoint; i < this.N_BOOL + this.N_DOUBLE; i++) {
			if (countGenes == currentNGenes) {
				// end of the individual plan reached.
				countGenes = 1;
				currentNGenes = nGenesIterator.next();
				minIndivCoef = Math.min(minIndivCoef,
						calculatePlanDurCoef(mate1PlanDuration, crossOverSurplus));
				crossOverSurplus = 0d;
				mate1PlanDuration = 0d;
			} else {
				countGenes++;
			}

			currentEpisodeDuration1 = ((DoubleGene) mate1Genes[i]).doubleValue();
			currentEpisodeDuration2 = ((DoubleGene) mate2Genes[i]).doubleValue();
			mate1PlanDuration += currentEpisodeDuration1;

			currentSurplus = currentEpisodeDuration2 - currentEpisodeDuration1;

			// normally, unecessary: to remove
			//if (currentSurplus < 0) {
			//	minPosDurationCoef = Math.min(
			//			minPosDurationCoef,
			//			-currentEpisodeDuration1 / currentSurplus);
			//}

			crossOverSurplus += currentSurplus;
		}

		// take the last individual plan into account
		minIndivCoef = Math.min(minIndivCoef,
			calculatePlanDurCoef(mate1PlanDuration, crossOverSurplus));

		//take both plan duration and positive duration into account
		// minIndivCoef = Math.min(minIndivCoef, minPosDurationCoef);

		return Math.min(1d, minIndivCoef);
	}

	private double calculatePlanDurCoef(
			final double mate1PlanDuration,
			final double crossOverSurplus) {
		if (Math.abs(crossOverSurplus) < EPSILON) {
			return 1d;
		} else {
			double upperLimit = (DAY_DURATION - mate1PlanDuration) / crossOverSurplus;
			return Math.max(0d, Math.min(1d, upperLimit));
		}
	}

	/**
	 * Performs a "GENOCOP-like" "Single arithmetic cross-over" on the double
	 * valued genes.
	 * each gene is crossed with probability 0.5
	 */
	private final void doDoubleSingleCrossOver(
			final IChromosome mate1,
			final IChromosome mate2) {
		DoubleGene gene1;
		DoubleGene gene2;
		double oldValue1;
		double oldValue2;
		double crossingCoef1;
		double crossingCoef2;

		// initialize a list of indices of the double genes, in random order.
		// the random order makes all duration genes having the same "status"
		List<Integer> indicesToCross = new ArrayList<Integer>(this.N_DOUBLE);
		for (int i = this.N_BOOL; i < this.N_BOOL + this.N_DOUBLE; i++) {
			indicesToCross.add(i);
		}
		Collections.shuffle(indicesToCross, (Random) this.randomGenerator);

		for (int crossingPoint : indicesToCross) {
			// swap with probability O.5
			if (this.randomGenerator.nextInt(2) == 1) {
				continue;
			}

			crossingCoef1 = singleCoCrossingCoef(
					mate1.getGenes(),
					mate2.getGenes(),
					crossingPoint);
			crossingCoef2 = singleCoCrossingCoef(
					mate2.getGenes(),
					mate1.getGenes(),
					crossingPoint);

			gene1 = (DoubleGene) mate1.getGene(crossingPoint);
			gene2 = (DoubleGene) mate2.getGene(crossingPoint);
			oldValue1 = gene1.doubleValue();
			oldValue2 = gene2.doubleValue();
			
			if (crossingCoef1 > EPSILON) {
				gene1.setAllele(
						(1 - crossingCoef1)*oldValue1 +
						crossingCoef1*oldValue2);
			}
			if (crossingCoef2 > EPSILON) {
				gene2.setAllele(
						crossingCoef2*oldValue1 +
						(1 - crossingCoef2)*oldValue2);
			}
		}
	}

	/**
	 * computes the "crossing coefficient" for a simple arithmetic CO.
	 * This corresponds to a random a such that the vectors 
	 * (x_1, x_2, ..., x_(i-1), (1 - a)x_i + a*y_i, x_(i+1), ..., x_n)
	 * and
	 * (y_1, y_2, ..., y_(i-1), (1 - a)y_i + a*x_i, y_(i+1), ..., y_n)
	 * respect the constraints.
	 */
	private double singleCoCrossingCoef(
			final Gene[] mate1Genes,
			final Gene[] mate2Genes,
			final int crossingPoint) {
		double mate1PlanDuration = 0d;
		double crossOverSurplus = 0d;
		double currentEpisodeDuration;
		Iterator<Integer> nGenesIterator = this.nDurationGenes.iterator();
		int currentNGenes=nGenesIterator.next();
		int countGenes = 0;
		boolean crossingPointIsPast = false;
		//double randomCoef = this.randomGenerator.nextDouble();
		double randomCoef = 1d;
		//double posDurCoef = 1d;
		double coef;
	
		for (int i=this.N_BOOL; i < this.N_BOOL + this.N_DOUBLE; i++) {
			if (countGenes == currentNGenes) {
				if (!crossingPointIsPast) {
					// end of an individual plan reached.
					countGenes = 0;
					currentNGenes = nGenesIterator.next();
					mate1PlanDuration = 0d;
				}
				else {
					coef =// Math.min(posDurCoef,
						(	calculatePlanDurCoef(
								mate1PlanDuration,
								crossOverSurplus));
					coef = Math.min(1d, coef);
					coef = Math.max(0d, coef);
					return randomCoef * coef;
				}
			}

			currentEpisodeDuration = ((DoubleGene) mate1Genes[i]).doubleValue();
			mate1PlanDuration += currentEpisodeDuration;

			if (i==crossingPoint) {
				crossOverSurplus = ((DoubleGene) mate2Genes[i]).doubleValue() -
					currentEpisodeDuration;
				// useless
				//if (crossOverSurplus < 0) {
				//	posDurCoef = -currentEpisodeDuration / crossOverSurplus;
				//}
				crossingPointIsPast = true;
			}
			countGenes++;
		}

		if (crossingPointIsPast) {
			// if the crossing point was in the last plan
			//coef = Math.min(posDurCoef,
			coef = (calculatePlanDurCoef(
					mate1PlanDuration,
					crossOverSurplus));
			coef = Math.min(1d, coef);
			coef = Math.max(0d, coef);
			return randomCoef * coef;
		}

		throw new RuntimeException("Single cross over coefficient computation failed!");
	}
}
