/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractTripModeShares30kmFromBellevue.java
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
package playground.thibautd.scripts;

import java.io.BufferedWriter;
import java.io.IOException;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.pt.PtConstants;

import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.utils.JointMainModeIdentifier;

/**
 * @author thibautd
 */
public class ExtractTripModeShares20kmFromBellevue {
	private static final Coord BELLEVUE_COORD = new CoordImpl( 683518 , 246836 );
	private static final double radius = 20000;
	private static final StageActivityTypes STAGES =
		new StageActivityTypesImpl(
				Arrays.asList(
					PtConstants.TRANSIT_ACTIVITY_TYPE,
					JointActingTypes.PICK_UP,
					JointActingTypes.DROP_OFF ) );
	private static final MainModeIdentifier MODE_IDENTIFIER = new JointMainModeIdentifier( new MainModeIdentifierImpl() );

	private static final double CROW_FLY_FACTOR = 1.2;

	private static enum OutputType { COUNT, DETAILED; }
	private static final OutputType OUTPUT_TYPE = OutputType.DETAILED;

	public static void main(final String[] args) throws IOException {
		final String plansFile = args[ 0 ];
		final String outputFile = args[ 1 ];
		// for V4 or distance computation
		final String networkFile = args.length > 2 ? args[ 2 ] : null;
		// useful for V4 only
		final String facilitiesFile = args.length > 3 ? args[ 3 ] : null;

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		if ( facilitiesFile != null ) new MatsimFacilitiesReader( (ScenarioImpl) scenario ).parse( facilitiesFile );
		if ( networkFile != null ) new MatsimNetworkReader( scenario ).parse( networkFile );
		new MatsimPopulationReader( scenario ).parse( plansFile );

		switch ( OUTPUT_TYPE ) {
			case COUNT:
				count( scenario , outputFile );
				break;
			case DETAILED:
				detailed( scenario , outputFile );
				break;
		default:
			throw new RuntimeException( ""+OUTPUT_TYPE );
		}
	}

	private static void detailed(
			final Scenario scenario,
			final String outputFile) throws IOException {
		final BufferedWriter writer = IOUtils.getBufferedWriter( outputFile );
		writer.write( "agentId\tmain_mode\ttotal_dist" );

		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			final Plan plan = person.getSelectedPlan();
			final Activity act = getHomeActivity( plan );
			if ( CoordUtils.calcDistance( act.getCoord() , BELLEVUE_COORD ) > radius ) continue;
			for ( Trip trip : TripStructureUtils.getTrips( plan , STAGES ) ) {
				final String mode = MODE_IDENTIFIER.identifyMainMode( trip.getTripElements() );
				writer.newLine();
				writer.write( person.getId()+"\t"+mode+"\t"+calcDist( trip , scenario.getNetwork() ) );
			}
		}

		writer.close();
	}

	private static double calcDist(final Trip trip, final Network network) {
		double dist = 0;

		for ( Leg l : trip.getLegsOnly() ) {
			final Route r = l.getRoute();
			if ( r instanceof NetworkRoute )  {
				dist += RouteUtils.calcDistance( (NetworkRoute) r , network );
			}
			else {
				// TODO: make configurable?
				dist += CROW_FLY_FACTOR *
					// TODO: use coord of activities
					CoordUtils.calcDistance(
							network.getLinks().get( r.getStartLinkId() ).getFromNode().getCoord(),
							network.getLinks().get( r.getEndLinkId() ).getToNode().getCoord() );
			}
		}

		return dist;
	}

	private static void count(
			final Scenario scenario,
			final String outputFile) throws IOException {
		final Map<String, Integer> counts = new TreeMap<String, Integer>();

		int total = 0;
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			final Plan plan = person.getSelectedPlan();
			final Activity act = getHomeActivity( plan );
			if ( CoordUtils.calcDistance( act.getCoord() , BELLEVUE_COORD ) > radius ) continue;
			for ( Trip trip : TripStructureUtils.getTrips( plan , STAGES ) ) {
				final String mode = MODE_IDENTIFIER.identifyMainMode( trip.getTripElements() );
				final Integer count = counts.get( mode );
				counts.put( mode , count == null ? 1 : count + 1 );
				total++;
			}
		}

		final BufferedWriter writer = IOUtils.getBufferedWriter( outputFile );
		writer.write( "mode\tcount\tshare" );
		for ( Map.Entry<String, Integer> count : counts.entrySet() ) {
			writer.newLine();
			writer.write( count.getKey()+"\t"+count.getValue()+"\t"+( count.getValue().doubleValue() / total ) );
		}
		writer.close();
	}

	private static Activity getHomeActivity(final Plan plan) {
		final Activity activity = (Activity) plan.getPlanElements().get( 0 );
		if ( !activity.getType().equals( "home" ) ) throw new IllegalArgumentException( ""+plan.getPlanElements() );
		return activity;
	}
}
