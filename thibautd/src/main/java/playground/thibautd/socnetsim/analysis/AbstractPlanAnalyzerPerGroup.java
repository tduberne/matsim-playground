/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractPlanAnalyzerPerGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.analysis;

import java.io.BufferedWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author thibautd
 */
public abstract class AbstractPlanAnalyzerPerGroup implements IterationEndsListener, ShutdownListener {
	private static final Logger log =
		Logger.getLogger(AbstractPlanAnalyzerPerGroup.class);

	/**
	 * THe plan sizes are plotted per group: this class aims at saying which agent
	 * pertains to each group (eg "agents in an household of 3 persons which have a car available")
	 */
	public static interface GroupIdentifier {
		public Id getGroup(Person person);
	}

	private final Scenario scenario;
	private final GroupIdentifier groupIdentifier;
	private final String fileName;
	private final BufferedWriter writer;

	private final HistoryPerGroup historyPerGroup = new HistoryPerGroup();

	public AbstractPlanAnalyzerPerGroup(
			final OutputDirectoryHierarchy controlerIO,
			final Scenario scenario,
			final GroupIdentifier groupIdentifier) {
		this.scenario = scenario;
		this.groupIdentifier = groupIdentifier;
		fileName = controlerIO.getOutputFilename( getStatName()+"Stats" );
		writer = IOUtils.getBufferedWriter( fileName +".dat.gz" );

		try {
			writer.write( "iter\tgroupId\tmin\tmax\tavg\texec" );
		} catch (IOException e) {
			// this is not fatal; do not crash
			log.error( "problem writing to file" , e );
		}
	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.writer.close();
		} catch (IOException e) {
			// this is not fatal, no need to crash
			log.error( "problem closing file for "+getClass().getSimpleName(), e );
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			final History history =
				historyPerGroup.getHistoryForGroup(
						groupIdentifier.getGroup( person ) );

			history.notifyIteration( event.getIteration() );
			fillStatsForPerson( history , person );
		}

		for (Map.Entry<Id, History> entry : historyPerGroup.getEntries()) {
			final Id groupId = entry.getKey();
			final History history = entry.getValue();
			dropDataFile( groupId , history );
			dropChart( groupId , history );
		}

		try {
			writer.flush();
		} catch (IOException e) {
			log.error( e );
		}
	}

	private void dropDataFile(
			final Id groupId,
			final History history) {
		final double[] iterations = history.getIterations();
		final int n = iterations.length - 1;
		final int iteration = (int) iterations[ n ];
		
		try {
			writer.newLine();
			writer.write(
					iteration+"\t"+
					groupId+"\t"+
					history.getAvgOfMinimums()[ n ]+"\t"+
					history.getAvgOfMaximums()[ n ]+"\t"+
					history.getAvgOfAverages()[ n ]+"\t"+
					history.getAvgOfExecuted()[ n ]);
		} catch (IOException e) {
			log.error( e );
		}
	}

	private void dropChart(
			final Id groupId,
			final History history) {
		final XYLineChart chart =
			new XYLineChart(
					getStatName()+" Statistics for "+groupId,
					"iteration",
					getStatName());

		chart.addSeries(
				"avg. minimum "+getStatName(),
				history.getIterations(),
				history.getAvgOfMinimums());
		chart.addSeries(
				"avg. max "+getStatName(),
				history.getIterations(),
				history.getAvgOfMaximums());
		chart.addSeries(
				"avg. of plans' average "+getStatName(),
				history.getIterations(),
				history.getAvgOfAverages());
		chart.addSeries(
				"avg. executed "+getStatName(),
				history.getIterations(),
				history.getAvgOfExecuted());
		chart.addMatsimLogo();
		chart.saveAsPng(this.fileName + "_" + groupId + ".png", 800, 600);
	}

	private void fillStatsForPerson(
			final History history,
			final Person person) {
		int count = 0;
		double maxStat = Double.NEGATIVE_INFINITY;
		double minStat = Double.POSITIVE_INFINITY;
		double executedStat = Double.NaN;
		double statsSum = 0;

		for (Plan plan : person.getPlans()) {
			final double stat = calcStat( plan );
			count++;

			if (stat > maxStat) maxStat = stat;
			if (stat < minStat) minStat = stat;
			if (plan.isSelected()) executedStat = stat;

			statsSum += stat;
		}

		if (count == 0) return;
		history.addAverage( statsSum / count );
		history.addMinimum( minStat );
		history.addMaximum( maxStat );
		history.addExecuted( executedStat );
	}

	protected abstract double calcStat(final Plan plan);

	protected abstract String getStatName();

	private static final class HistoryPerGroup {
		private final Map<Id, History> historyPerGroup =
			new LinkedHashMap<Id, History>();

		public History getHistoryForGroup(final Id groupId) {
			History history = historyPerGroup.get( groupId );

			if (history == null) {
				history = new History();
				historyPerGroup.put( groupId , history );
			}

			return history;
		}

		public Iterable<Map.Entry<Id, History>> getEntries() {
			return historyPerGroup.entrySet();
		}
	}

	private static final class History {
		private final List<Integer> iterationNumbers = new ArrayList<Integer>();

		private double[] sumsAverages = new double[0];
		private int[] countsAverages = new int[0];

		private double[] sumsMaximums = new double[0];
		private int[] countsMaximums = new int[0];

		private double[] sumsMinimums = new double[0];
		private int[] countsMinimums = new int[0];

		private double[] sumsExecuted = new double[0];
		private int[] countsExecuted = new int[0];


		public void notifyIteration(final int iteration) {
			if (iterationNumbers.contains( iteration )) {
				if ( iterationNumbers.indexOf( iteration ) != iterationNumbers.size() - 1 ) {
					throw new IllegalArgumentException(
							iteration+" is not the last element of "+iterationNumbers );
				}
				return;
			}

			iterationNumbers.add( iteration );

			sumsAverages = Arrays.copyOf( sumsAverages , sumsAverages.length + 1 );
			countsAverages = Arrays.copyOf( countsAverages , countsAverages.length + 1 );

			sumsMaximums = Arrays.copyOf( sumsMaximums , sumsMaximums.length + 1 );
			countsMaximums = Arrays.copyOf( countsMaximums , countsMaximums.length + 1 );

			sumsMinimums = Arrays.copyOf( sumsMinimums , sumsMinimums.length + 1 );
			countsMinimums = Arrays.copyOf( countsMinimums , countsMinimums.length + 1 );

			sumsExecuted = Arrays.copyOf( sumsExecuted , sumsExecuted.length + 1 );
			countsExecuted = Arrays.copyOf( countsExecuted , countsExecuted.length + 1 );
		}

		public void addAverage(final double value) {
			final int l = sumsAverages.length;
			assert l == countsAverages.length;
			sumsAverages[ l-1 ] += value;
			countsAverages[ l-1 ]++;
		}

		public void addMaximum(final double value) {
			final int l = sumsMaximums.length;
			assert l == countsMaximums.length;
			sumsMaximums[ l-1 ] += value;
			countsMaximums[ l-1 ]++;
		}

		public void addMinimum(final double value) {
			final int l = sumsMinimums.length;
			assert l == countsMinimums.length;
			sumsMinimums[ l-1 ] += value;
			countsMinimums[ l-1 ]++;
		}

		public void addExecuted(final double value) {
			final int l = sumsExecuted.length;
			assert l == countsExecuted.length;
			sumsExecuted[ l-1 ] += value;
			countsExecuted[ l-1 ]++;
		}

		public double[] getAvgOfAverages() {
			assert sumsAverages.length == countsAverages.length;
			final double[] value = Arrays.copyOf( sumsAverages , sumsAverages.length );

			for (int i=0; i < value.length; i++) {
				value[ i ] /= countsAverages[ i ];
			}

			return value;
		}

		public double[] getAvgOfMaximums() {
			assert sumsMaximums.length == countsMaximums.length;
			final double[] value = Arrays.copyOf( sumsMaximums , sumsMaximums.length );

			for (int i=0; i < value.length; i++) {
				value[ i ] /= countsMaximums[ i ];
			}

			return value;
		}

		public double[] getAvgOfMinimums() {
			assert sumsMinimums.length == countsMinimums.length;
			final double[] value = Arrays.copyOf( sumsMinimums , sumsMinimums.length );

			for (int i=0; i < value.length; i++) {
				value[ i ] /= countsMinimums[ i ];
			}

			return value;
		}

		public double[] getAvgOfExecuted() {
			assert sumsExecuted.length == countsExecuted.length;
			final double[] value = Arrays.copyOf( sumsExecuted , sumsExecuted.length );

			for (int i=0; i < value.length; i++) {
				value[ i ] /= countsExecuted[ i ];
			}

			return value;
		}

		public double[] getIterations() {
			final double[] iters = new double[ iterationNumbers.size() ];
			
			int i=0;
			for (Integer d : iterationNumbers) iters[ i++ ] = d;

			return iters;
		}
	}
}

