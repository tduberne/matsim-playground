/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkedPopulationDilutionUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.transformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.population.SocialNetworkImpl;

/**
 * @author thibautd
 */
public class SocialNetworkedPopulationDilutionUtils {
	private static final Logger log =
		Logger.getLogger(SocialNetworkedPopulationDilutionUtils.class);

	/**
	 * Dilutes the population of the scenario, by retaining only agents passing
	 * by the area defined by center and radius, as well as their social contacts.
	 * TODO: try also only retaining social contacts of agents living or working in the zone
	 * (not agents passing by).
	 */
	public static void dilute(
			final Scenario scenario,
			final Coord center,
			final double radius ) {
		log.info( "Start dilution with center "+center+" and radius "+radius );
		final Set<Id> personsToKeep = new HashSet<Id>();
		fillSetWithIntersectingPersons(
				personsToKeep,
				scenario,
				center,
				radius );
		fillSetWithAltersOfSet(
				personsToKeep,
				scenario );
		final Collection<Id> pruned =
			prunePopulation(
				scenario,
				personsToKeep );
		pruneSocialNetwork( pruned , scenario );
		log.info( "Finished dilution." );
	}

	/**
	 * Dilutes the population of the scenario, by retaining only agents passing
	 * by the area defined by center and radius, as well as their social contacts.
	 * For social contacts not part of the "dilution", they are only kept if they,
	 * as well as the ego, have a leisure activity.
	 */
	public static void diluteLeisureOnly(
			final Scenario scenario,
			final Coord center,
			final double radius ) {
		log.info( "Start dilution with center "+center+" and radius "+radius );
		final Set<Id> personsToKeep = new HashSet<Id>();
		fillSetWithIntersectingPersons(
				personsToKeep,
				scenario,
				center,
				radius );
		fillSetWithLeisureAltersOfSet(
				personsToKeep,
				scenario );
		final Collection<Id> pruned =
			prunePopulation(
				scenario,
				personsToKeep );
		pruneSocialNetwork( pruned , scenario );
		log.info( "Finished dilution." );
	}

	private static void pruneSocialNetwork(
			final Collection<Id> toPrune,
			final Scenario scenario) {
		final SocialNetworkImpl sn = (SocialNetworkImpl)
			scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		
		log.info( "Pruning of the social network begins." );
		log.info( sn.getEgos().size()+" egos." );

		for ( Id id : toPrune ) sn.removeEgo( id );

		log.info( "Pruning of the social network finished." );
		log.info( sn.getEgos().size()+" egos remaining." );
	}

	private static Collection<Id> prunePopulation(
			final Scenario scenario,
			final Set<Id> personsToKeep) {
		final Population population = scenario.getPopulation();
		log.info( "Actual pruning of the population begins." );
		log.info( "Population size: "+population.getPersons().size() );
		log.info( "Remaining persons to keep: "+personsToKeep.size() );

		// XXX this is not guaranteed to remain feasible...
		final Iterator<Id> popit = population.getPersons().keySet().iterator();
		final Collection<Id> pruned = new ArrayList<Id>();
		while ( popit.hasNext() ) {
			final Id curr = popit.next();
			if ( !personsToKeep.remove( curr ) ) {
				popit.remove();
				pruned.add( curr );
			}
		}

		log.info( "Actual pruning of the population finished." );
		log.info( "Population size: "+population.getPersons().size() );
		log.info( "Pruned: "+pruned.size() );
		log.info( "Remaining persons to keep: "+personsToKeep.size() );

		return pruned;
	}

	private static void fillSetWithAltersOfSet(
			final Set<Id> personsToKeep,
			final Scenario scenario) {
		log.info( "Search for alters of identified persons" ); 

		final SocialNetwork sn = (SocialNetwork)
			scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		if ( !sn.isReflective() ) throw new IllegalArgumentException( "results undefined with unreflexive network." );

		final Collection<Id> alters = new ArrayList<Id>();
		for ( Id ego : personsToKeep ) alters.addAll( sn.getAlters( ego ) );

		personsToKeep.addAll( alters );

		log.info( "Finished search for alters of identified persons" ); 
		log.info( personsToKeep.size()+" agents identified in total over "+scenario.getPopulation().getPersons().size() );
	}

	private static void fillSetWithLeisureAltersOfSet(
			final Set<Id> personsToKeep,
			final Scenario scenario) {
		log.info( "Search for LEISURE alters of identified persons" ); 

		final SocialNetwork sn = (SocialNetwork)
			scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		if ( !sn.isReflective() ) throw new IllegalArgumentException( "results undefined with unreflexive network." );

		final Set<Id> withLeisure = identifyAgentsWithLeisure( scenario );

		final Collection<Id> alters = new ArrayList<Id>();
		for ( Id ego : personsToKeep ) {
			if ( !withLeisure.contains( ego ) ) continue; // only consider ties potentially activated

			for ( Id alter : sn.getAlters( ego ) ) {
				if ( !withLeisure.contains( alter ) ) continue; // only consider ties potentially activated
				alters.add( alter );
			}
		}

		personsToKeep.addAll( alters );

		log.info( "Finished search for alters of identified persons" ); 
		log.info( personsToKeep.size()+" agents identified in total over "+scenario.getPopulation().getPersons().size() );
	}

	private static Set<Id> identifyAgentsWithLeisure(final Scenario scenario) {
		final Set<Id> agents = new HashSet<Id>();
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			final Plan plan = person.getSelectedPlan();
			assert plan != null : person.getId();

			for ( Activity act : TripStructureUtils.getActivities( plan , EmptyStageActivityTypes.INSTANCE ) ) {
				// TODO: less hardcoded
				if ( act.getType().equals( "leisure" ) ) {
					agents.add( person.getId() );
					break;
				}
			}
		}

		return agents;
	}

	private static void fillSetWithIntersectingPersons(
			final Set<Id> personsToKeep,
			final Scenario scenario,
			final Coord center,
			final double radius) {
		log.info( "Search for intersecting persons" );
		log.warn( "Only using crowfly intersection (not network routes)" );

		for ( Person p : scenario.getPopulation().getPersons().values() ) {
			if ( accept( p , center , radius ) ) personsToKeep.add( p.getId() );
		}

		log.info( "Finished search for intersecting persons" );
		log.info( personsToKeep.size()+" agents identified over "+scenario.getPopulation().getPersons().size() );
	}

	private static boolean accept(
			final Person p,
			final Coord center,
			final double radius) {
		Coord lastCoord = null;

		for ( Activity activity : TripStructureUtils.getActivities( p.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE ) ) {
			if ( activity.getCoord() == null ) throw new NullPointerException( "no coord for activity "+activity+" for person "+p );

			if ( CoordUtils.calcDistance( center , activity.getCoord() ) < radius ) return true;

			if ( lastCoord != null &&
					CoordUtils.distancePointLinesegment(
						lastCoord , activity.getCoord(),
						center ) < radius ) return true;
			lastCoord = activity.getCoord();
		}

		return false;
	}
}


