/* *********************************************************************** *
 * project: org.matsim.*
 * DataPloter.java
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
package playground.thibautd.analysis.joinabletripsidentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.charts.ChartUtil;

import playground.thibautd.utils.BoxAndWhiskersChart;
import playground.thibautd.utils.WrapperChartUtil;
import playground.thibautd.utils.XYLineHistogramDataset;

/**
 * Class responsible for creating relevant plots from the data
 * contained in a {@link JoinableTrips} instance.
 *
 * @author thibautd
 */
public class DataPloter {
	private final JoinableTrips trips;

	public DataPloter(final JoinableTrips trips) {
		this.trips = trips;
	}

	public ChartUtil getBasicBoxAndWhiskerChart(
			final PassengerFilter filter,
			final DriverTripValidator validator) {
		List<JoinableTrips.TripRecord> filteredTrips =
			filter.filterRecords(trips);
		validator.setJoinableTrips(trips);

		String title = "Number of possible joint trips per departure time\n"+
			filter.getConditionDescription()+"\n"+
			validator.getConditionDescription();

		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
			title,
			"time of day (h)",
			"number of joinable trips",
			1);

		for (JoinableTrips.TripRecord trip : filteredTrips) {
			int count = 0;

			for (JoinableTrips.JoinableTrip driverTrip : trip.getJoinableTrips()) { 
				if (validator.isValid(driverTrip)) {
					count++;
				}
			}

			chart.add(trip.getDepartureTime() / 3600d, count);
		}

		return chart;
	}

	public ChartUtil getBoxAndWhiskerChartPerTripLength(
			final PassengerFilter filter,
			final DriverTripValidator validator,
			final Network network) {
		List<JoinableTrips.TripRecord> filteredTrips =
			filter.filterRecords(trips);
		validator.setJoinableTrips(trips);

		String title = "Number of possible joint trips per trip length\n"+
			filter.getConditionDescription()+"\n"+
			validator.getConditionDescription();

		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
			title,
			"trip length (km)",
			"number of joinable trips",
			1);

		for (JoinableTrips.TripRecord trip : filteredTrips) {
			int count = 0;

			for (JoinableTrips.JoinableTrip driverTrip : trip.getJoinableTrips()) { 
				if (validator.isValid(driverTrip)) {
					count++;
				}
			}

			double tripLength = trip.getDistance(network);

			chart.add(tripLength / 1000d, count);
		}

		return chart;
	}

	/**
	 * @param conditions a list of conditions to compare. they must implement
	 * the equals and hashCode methods.
	 */
	public ChartUtil getTwofoldConditionComparisonChart(
			final PassengerFilter filter,
	 		final List<? extends TwofoldTripValidator> conditions) {
		String title = "Number of possible joint trips for different criteria";

		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
		Collections.sort(conditions, new ConditionComparator());
		
		for (TwofoldTripValidator validator : conditions) {
			List<JoinableTrips.TripRecord> filteredTrips =
				filter.filterRecords(trips);
			validator.setJoinableTrips(trips);

			List<Integer> counts = new ArrayList<Integer>();
			for (JoinableTrips.TripRecord trip : filteredTrips) {
				int count = 0;

				for (JoinableTrips.JoinableTrip driverTrip : trip.getJoinableTrips()) { 
					if (validator.isValid(driverTrip)) {
						count++;
					}
				}

				counts.add(count);
			}
			dataset.add(counts, validator.getFirstCriterion(), validator.getSecondCriterion());
		}

		JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
				title, "", "number of possible joint trips", dataset, true);

		return new WrapperChartUtil(chart);
	}

	/**
	 * @return a chart displaying the number of trips fulfilling the filter
	 * conditions, per departure time.
	 */
	public ChartUtil getTripsForConditions(
			final List<PassengerFilter> filters) {
		double binWidth = 1d / 4d;
		XYLineHistogramDataset dataset = new XYLineHistogramDataset(binWidth);
	
		for (PassengerFilter filter : filters) {
			List<Double> departureTimes = new ArrayList<Double>();
			List<JoinableTrips.TripRecord> filteredTrips =
				filter.filterRecords(trips);
			
			for (JoinableTrips.TripRecord record : filteredTrips) {
				departureTimes.add(record.getDepartureTime() / 3600d);
			}

			dataset.addSeries(filter.toString(), departureTimes);
		
		}

		JFreeChart chart = ChartFactory.createXYLineChart(
				"departures histogram",
				"time (h)",
				"number of departure",
				dataset,
				PlotOrientation.VERTICAL,
				true, // display legend
				false, //no tooltips
				false); // no URLS

		return new WrapperChartUtil(chart);
	}

	public ChartUtil getTripsForCondition(
			final PassengerFilter filter) {
		List<PassengerFilter> filters = new ArrayList<PassengerFilter>(1);
		filters.add(filter);
		return getTripsForConditions(filters);
	}

	// /////////////////////////////////////////////////////////////////////////
	// nested interface
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Provides a way to select the passenger trips actually taken into account.
	 * This allows for example to only consider passenger doing a home work commute
	 */
	public interface PassengerFilter {
		/**
		 * @param trips the instance containing raw data
		 * @return a list of passenger trips satisfying the required criterion
		 */
		public List<JoinableTrips.TripRecord> filterRecords(final JoinableTrips trips);

		public String getConditionDescription();
	}

	/**
	 * Provides a way of checking whether a driver trip obeys to some criterion
	 * (for example, focus on a given acceptability condition and only consider
	 * commuting drivers).
	 */
	public interface DriverTripValidator {
		/**
		 * Called before any validation work. Can be used to get additionnal
		 * information about the driver trip.
		 */
		public void setJoinableTrips(final JoinableTrips joinableTrips);

		/**
		 * @param driverTrip the potential driver trip
		 * @return true if the driver trip is to be counted
		 */
		public boolean isValid(final JoinableTrips.JoinableTrip driverTrip);

		public String getConditionDescription();
	}

	/**
	 * Provides additional information for a DriverTripValidator which uses at least two criterions
	 * (as for example distance and time difference) to proceed to filtering
	 */
	public interface TwofoldTripValidator extends DriverTripValidator {
		public Comparable getFirstCriterion();
		public Comparable getSecondCriterion();
	}

	private static class ConditionComparator implements Comparator<TwofoldTripValidator> {
		@Override
		public int compare(
				final TwofoldTripValidator v1,
				final TwofoldTripValidator v2) {
			int comp = v1.getSecondCriterion().compareTo(v2.getSecondCriterion());
			if (comp == 0) comp = v1.getFirstCriterion().compareTo(v2.getFirstCriterion());
			return comp;
		}
	}


}

