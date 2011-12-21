/* *********************************************************************** *
 * project: org.matsim.*
 * PlotData.java
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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.Module;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.collections.Tuple;

import playground.thibautd.analysis.joinabletripsidentifier.DataPloter.DriverTripValidator;
import playground.thibautd.analysis.joinabletripsidentifier.DataPloter.PassengerFilter;
import playground.thibautd.analysis.joinabletripsidentifier.DataPloter.TwofoldTripValidator;
import playground.thibautd.analysis.joinabletripsidentifier.JoinableTrips.JoinableTrip;
import playground.thibautd.analysis.joinabletripsidentifier.JoinableTrips.TripRecord;
import playground.thibautd.utils.MoreIOUtils;
import playground.thibautd.utils.charts.ChartsAxisUnifier;

/**
 * Executable class able to plot statistics about exported XML data
 *
 * @author thibautd
 */
public class PlotData {
	private static final Logger log =
		Logger.getLogger(PlotData.class);

	// for lisibility, charts per distance with a maximal
	// distance are produced. For Zürich, distances longer than 30km
	// do not make sense due to the way the population is defined.
	private static double LONGER_DIST = 25 * 1000;
	private static double LONGER_DRIVER_DIST = 25 * 1000;

	// config file: data dump, conditions (comme pour extract)
	private static final String MODULE = "jointTripIdentifier";
	private static final String DIST = "acceptableDistance_.*";
	private static final String TIME = "acceptableTime_";
	private static final String DIR = "outputDir";
	private static final String XML_FILE = "xmlFile";
	//private static final int WIDTH = 1024;
	//private static final int HEIGHT = 800;
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;

	public static void main(final String[] args) {
		// TODO: define a set of minimal distances
		String configFile = args[0];
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		Module module = config.getModule(MODULE);
		String outputDir = module.getValue(DIR);
		String xmlFile = module.getValue(XML_FILE);

		MoreIOUtils.initOut( outputDir );

		List<ConditionValidator> conditions = new ArrayList<ConditionValidator>();

		Map<String, String> params = module.getParams();

		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (entry.getKey().matches(DIST)) {
				double dist = Double.parseDouble(entry.getValue());
				String num = entry.getKey().split("_")[1];
				double time = Double.parseDouble(params.get(TIME + num));
				conditions.add(new ConditionValidator(dist, time));
			}
		}

		// run ploting function and export
		JoinableTripsXmlReader reader = new JoinableTripsXmlReader();
		reader.parse(xmlFile);
		DataPloter ploter = new DataPloter(reader.getJoinableTrips());
		CommutersFilter filter = new CommutersFilter(network, -1, -1);
		CommutersFilter shortFilter = new CommutersFilter(network, -1, LONGER_DIST);
		ShortDriverTripValidator shortDriverTripValidator =
			new ShortDriverTripValidator( network , LONGER_DRIVER_DIST );

		List<ChartsAxisUnifier> unifiers = new ArrayList<ChartsAxisUnifier>();
		ChartsAxisUnifier perDistanceUnifier = new ChartsAxisUnifier( false , true );
		unifiers.add( perDistanceUnifier );
		ChartsAxisUnifier perTimeUnifier = new ChartsAxisUnifier( false , true );
		unifiers.add( perTimeUnifier );
		ChartsAxisUnifier passengersPerDriverUnifier = new ChartsAxisUnifier( false , true );
		unifiers.add( passengersPerDriverUnifier );

		List< Tuple< String , ChartUtil > > charts = 
			 new ArrayList< Tuple< String , ChartUtil > >();

		int count = 0;
		for (ConditionValidator condition : conditions) {
			log.info("creating charts for condition "+condition);
			count++;

			// number of joinable trips per passenger
			// -----------------------------------------------------------------
			ChartUtil chart = ploter.getBasicBoxAndWhiskerChart(
					filter,
					condition);
			perTimeUnifier.addChart( chart );
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+count+"-TimePlot",
						chart) );

			chart = ploter.getBoxAndWhiskerChartPerTripLength(
					filter,
					condition,
					network);
			perDistanceUnifier.addChart( chart );
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+count+"-DistancePlot",
						chart) );

			chart = ploter.getBoxAndWhiskerChartPerTripLength(
					shortFilter,
					condition,
					network);
			perDistanceUnifier.addChart( chart );
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+count+"-DistancePlot-short",
						chart) );

			// Number of possible passenger per driver
			// -----------------------------------------------------------------
			chart = ploter.getBoxAndWhiskerChartNPassengersPerDriverTripLength(
					filter,
					condition,
					network);
			passengersPerDriverUnifier.addChart( chart );
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+count+"-nPassengers-per-drivers",
						chart) );

			shortDriverTripValidator.setValidator( condition );
			chart = ploter.getBoxAndWhiskerChartNPassengersPerDriverTripLength(
					filter,
					shortDriverTripValidator,
					network);
			passengersPerDriverUnifier.addChart( chart );
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+count+"-nPassengers-per-drivers-short",
						chart) );


			// VIA: home and work locations of passengers and drivers
			// -----------------------------------------------------------------
			List<Coord> locations = ploter.getMatchingLocations(
					filter,
					condition,
					network,
					true,
					true,
					"h.*");
			ploter.writeViaXy(
					locations,
					outputDir+condition.getFirstCriterion()+"-"
						+condition.getSecondCriterion()+"-homeLocations.xy");

			locations = ploter.getMatchingLocations(
					filter,
					condition,
					network,
					true,
					true,
					"w.*");
			ploter.writeViaXy(
					locations,
					outputDir+condition.getFirstCriterion()+"-"
						+condition.getSecondCriterion()+"-workLocations.xy");
		}


		// "global" (multicondition) charts
		{
			// number of joint trips per condition
			// -----------------------------------------------------------------
			ChartUtil chart = ploter.getTwofoldConditionComparisonChart(filter, conditions);
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+"comparisonPlot",
						chart) );

			// departure histogram for the filtered passenger trips
			// -----------------------------------------------------------------
			chart = ploter.getTripsForCondition(filter);
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+"departuresPerTimeSlotPlot",
						chart) );

			// Proportion of passengers with joint trip
			// -----------------------------------------------------------------
			chart = ploter.getTwoFoldConditionProportionOfPassengers(filter, conditions);
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+"proportionOfPotentialRideSharers",
						chart) );
		}

		// format and save
		// ---------------------------------------------------------------------
		for (ChartsAxisUnifier unifier : unifiers) {
			unifier.applyUniformisation();
		}
		for (Tuple<String, ChartUtil> chart : charts) {
			ChartUtil chartUtil = chart.getSecond();
			chartUtil.saveAsPng(
					chart.getFirst()+".png",
					WIDTH,
					HEIGHT);
			chartUtil.getChart().setTitle( "" );
			chartUtil.saveAsPng(
					chart.getFirst()+"-no-title.png",
					WIDTH,
					HEIGHT);
		}
	}
}

class CommutersFilter implements PassengerFilter {
	private static final String WORK_REGEXP = "w.*";
	private static final String HOME_REGEXP = "h.*";

	private final Network network;
	private final double minDistance;
	private final double maxDistance;

	/**
	 * @param network the network to use to compute distance information
	 * @param minTravelDistance the minimal trip length for a trip to be included
	 * in the list of trips to treat. Negative or 0-valued means no lower bound
	 * @param maxTravelDistance the maximal trip length for a trip to be included
	 * in the list of trips to treat. Negative or 0-valued means no upper bound
	 */
	public CommutersFilter(
			final Network network,
			final double minTravelDistance,
			final double maxTravelDistance) {
		this.network = network;
		this.minDistance = minTravelDistance;
		this.maxDistance = maxTravelDistance;
	}

	@Override
	public List<TripRecord> filterRecords(final JoinableTrips trips) {
		List<TripRecord> filtered = new ArrayList<TripRecord>();

		for (TripRecord record : trips.getTripRecords().values()) {
			// only add commuters
			if ( (record.getOriginActivityType().matches(HOME_REGEXP) &&
					 record.getDestinationActivityType().matches(WORK_REGEXP)) ||
					(record.getDestinationActivityType().matches(HOME_REGEXP) &&
					 record.getOriginActivityType().matches(WORK_REGEXP)) ) {
				// check for distance
				if (( (minDistance <= 0) || (minDistance <= record.getDistance(network)) ) &&
				 ( (maxDistance <= 0) || (maxDistance >= record.getDistance(network)) )) {
					// check for mode (pt simulation makes results difficult to interpret otherwise)
					if ( record.getMode().equals( TransportMode.car ) ) {
						filtered.add(record);
					 }
				}
			}
		}

		return filtered;
	}

	@Override
	public String getConditionDescription() {
		return "commuter passengers only"+
			(minDistance > 0 ?
				 ", trips longer than "+minDistance+"m" :
				 "")+
			(maxDistance > 0 ?
				 ", trips shorter than "+maxDistance+"m" :
				 "");
	}

	public String toString() {
		return getConditionDescription();
	}
}

class ConditionValidator implements TwofoldTripValidator {
	private final AcceptabilityCondition condition;

	public ConditionValidator(final double distance, final double time) {
		this.condition = new AcceptabilityCondition(distance, time);
	}

	@Override
	public void setJoinableTrips(final JoinableTrips joinableTrips) {}

	@Override
	public boolean isValid(final JoinableTrip driverTrip) {
		return driverTrip.getFullfilledConditions().contains(condition);
	}

	@Override
	public String getConditionDescription() {
		return "all drivers\n"+getTailDescription();
	}

	public String getTailDescription() {
		return "acceptable distance = "+condition.getDistance()+" m"+
			"\nacceptable time = "+(condition.getTime()/60d)+" min";
	}

	@Override
	public String toString() {
		return getConditionDescription();
	}

	@Override
	public Comparable getFirstCriterion() {
		return new Label(condition.getDistance(), "", "m");
	}

	@Override
	public Comparable getSecondCriterion() {
		return new Label(condition.getTime()/60d, "", "min");
	}

	@Override
	public boolean equals(final Object object) {
		if ( !(object instanceof ConditionValidator) ) {
			return false;
		}

		AcceptabilityCondition otherCondition = ((ConditionValidator) object).condition;
		return condition.equals(otherCondition);
	}

	@Override
	public int hashCode() {
		return condition.hashCode();
	}

	private static class Label implements Comparable {
		private final double value;
		private final String prefix;
		private final String suffix;

		public Label(
				final double value,
				final String prefix,
				final String suffix) {
			this.value = value;
			this.prefix = prefix.intern();
			this.suffix = suffix.intern();
		}

		/**
		 * if the prefix and suffix are the same, compares the value; otherwise,
		 * compares string representation.
		 */
		@Override
		public int compareTo(final Object o) {
			if ( ((Label) o).prefix.equals(prefix) && ((Label) o).suffix.equals(suffix) ) {
				return Double.compare(value, ((Label) o).value);
			}
			return toString().compareTo(o.toString());
		}

		@Override
		public String toString() {
			return prefix + value + suffix;
		}

		@Override
		public int hashCode() {
			return toString().hashCode();
		}

		@Override
		public boolean equals(final Object o) {
			return toString().equals(o.toString());
		}
	}
}

/**
 * Wraps a condition validator, and only accepts trips valid for this validator
 * and which obey to a max-distance criterion.
 */
class ShortDriverTripValidator implements DriverTripValidator {
	private final double maxDist;
	private JoinableTrips trips = null;
	private ConditionValidator validator = null;
	private final Network network;

	public ShortDriverTripValidator(
			final Network network,
			final double maxDist) {
		this.network = network;
		this.maxDist = maxDist;
	}

	@Override
	public void setJoinableTrips(final JoinableTrips joinableTrips) {
		trips = joinableTrips;
	}

	public void setValidator( final ConditionValidator validator ) {
		this.validator = validator;
	}

	@Override
	public boolean isValid(final JoinableTrip driverTrip) {
		return validator.isValid( driverTrip ) && trips.getTripRecords().get(
				driverTrip.getTripId() ).getDistance( network ) <= maxDist;
	}

	@Override
	public String getConditionDescription() {
		return "maximum driver trips distance: "+maxDist+"\n"+validator.getTailDescription();
	}
}
