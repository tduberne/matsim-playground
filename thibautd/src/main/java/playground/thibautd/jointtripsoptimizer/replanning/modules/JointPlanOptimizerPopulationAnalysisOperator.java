/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerPopulationAnalysisOperator.java
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

import java.io.File;
import java.io.IOException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.BoxAndWhiskerCalculator;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerXYDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import org.jgap.GeneticOperator;
import org.jgap.Population;

/**
 * "fake" genetic operator for displaying information about the population.
 * CAUTION: generates large amounts of files at the root of the project
 * @author thibautd
 */
public class JointPlanOptimizerPopulationAnalysisOperator implements GeneticOperator {
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerPopulationAnalysisOperator.class);


	private static final long serialVersionUID = 1L;

	static int count = 0;
	private final int maxIters;
	private final JointPlanOptimizerJGAPConfiguration jgapConfig;
	private final String fileName;
	private final int populationSize;
	private final int chromosomeLength;
	private int width = 600;
	private int height = 300;



	//private final List<BoxAndWhiskerItem> boxes;
	private final DefaultBoxAndWhiskerCategoryDataset boxes;

	public JointPlanOptimizerPopulationAnalysisOperator(
			JointPlanOptimizerJGAPConfiguration jgapConfig,
			int maxIters,
			String outputPath) {
		this.jgapConfig = jgapConfig;
		this.populationSize = jgapConfig.getPopulationSize();
		this.chromosomeLength = jgapConfig.getChromosomeSize();
		this.maxIters = maxIters;
		//this.boxes = new ArrayList<BoxAndWhiskerItem>(maxIters);
		int currentCount = count++;
		boxes = new DefaultBoxAndWhiskerCategoryDataset();
		fileName = outputPath+"/fitnessAnalysis-"+currentCount+".png";
	}

	@Override
	public void operate(
			final Population a_population,
			final List a_candidateChromosome
			) {
		List<Double> fitnesses = new ArrayList<Double>(this.populationSize);
		String seriesName = "fitness: population="+this.populationSize+
			", chomosome size="+a_population.getChromosome(0).size();
		int iterNumber = this.jgapConfig.getGenerationNr() + 1;

		for (int i = 0; i < this.populationSize; i++) {
			fitnesses.add(a_population.getChromosome(i).getFitnessValue());
		}

		this.boxes.add(
				BoxAndWhiskerCalculator.calculateBoxAndWhiskerStatistics(fitnesses),
				seriesName,
				iterNumber);

		//if (iterNumber == this.maxIters) {
		//	outputFitnessBoxPlots();
		//}
	}

	/**
	 * responsible of writing the graphics to file.
	 */
	@Override
	public void finalize() throws Throwable {
		super.finalize();
		outputFitnessBoxPlots();
	}

	private void outputFitnessBoxPlots() {
		//log.info("writing fitness chart to file...");

		String title = "fitness: population="+this.populationSize+
			", chomosome size="+this.chromosomeLength;
		String xLabel = "iteration";
		String yLabel = "fitness";
		boolean legend = false;

		JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
				title, xLabel, yLabel, this.boxes, legend);

		try {
			ChartUtilities.saveChartAsPNG(new File(fileName), chart, width, height);
		} catch (IOException e) {
		}
		//log.info("writing fitness chart to file... DONE");
	}
}

