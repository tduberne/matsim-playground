/* *********************************************************************** *
 * project: org.matsim.*
 * AllocateVehicleToSubtourAtGroupLevelAlgorithm.java
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
package playground.thibautd.socnetsim.sharedvehicles.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;

import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;

/**
 * Allocates vehicles to legs with vehicle ressources, trying to minimize
 * number of agents using the same vehicle.
 * Each agent gets allocated exactly one vehicle.
 * No attempt is made to optimize the allocation given time.
 * @author thibautd
 */
public class AllocateVehicleToPlansInGroupPlanAlgorithm implements GenericPlanAlgorithm<GroupPlans> {
	private static final Logger log =
		Logger.getLogger(AllocateVehicleToPlansInGroupPlanAlgorithm.class);

	private final Random random;
	private final VehicleRessources vehicleRessources;
	private final String mode;
	private final boolean allowNullRoutes;

	/**
	 * @param random the random generator to use
	 * @param vehicleRessources the vehicles
	 * @param mode the mode for which to allocate vehicles
	 * @param allowNullRoutes if true, when a leg of mode
	 * <tt>mode</tt> has a null route, a new empty route
	 * will be created to receive the vehicle Id. If false,
	 * a null route will result in a exception.
	 */
	public AllocateVehicleToPlansInGroupPlanAlgorithm(
			final Random random,
			final VehicleRessources vehicleRessources,
			final String mode,
			final boolean allowNullRoutes) {
		this.random = random;
		this.vehicleRessources = vehicleRessources;
		this.mode = mode;
		this.allowNullRoutes = allowNullRoutes;
	}

	@Override
	public void run(final GroupPlans plan) {
		final List<Plan> plansWithVehicles = getPlansWithVehicles( plan );

		allocateOneVehiclePerPlan( plansWithVehicles );
	}

	private List<Plan> getPlansWithVehicles(final GroupPlans groupPlan) {
		final List<Plan> plans = new ArrayList<Plan>();

		for ( Plan p : groupPlan.getAllIndividualPlans() ) {
			for ( PlanElement pe : p.getPlanElements() ) {
				if ( !(pe instanceof Leg) ) continue;
				final Leg leg = (Leg) pe;

				if ( !mode.equals( leg.getMode() ) ) continue;
				if ( !( ( allowNullRoutes && leg.getRoute() == null ) ||
						( leg.getRoute() instanceof NetworkRoute ) ) ) {
					throw new RuntimeException( "route for mode "+mode+" has non-network route "+leg.getRoute() );
				}
				plans.add( p );
				break;
			}
		}

		return plans;
	}

	private void allocateOneVehiclePerPlan(final List<Plan> plansWithVehicles) {
		// make the allocation random by shuffling the order in which the plans
		// are examined
		Collections.shuffle( plansWithVehicles , random );

		final Map<Id, Id> allocation = new LinkedHashMap<Id, Id>();
		allocate( plansWithVehicles , allocation );

		for ( Plan p : plansWithVehicles ) {
			final Id v = allocation.get( p.getPerson().getId() );
			assert v != null;

			for ( PlanElement pe : p.getPlanElements() ) {
				if ( !(pe instanceof Leg) ) continue;
				final Leg leg = (Leg) pe;

				if ( !mode.equals( leg.getMode() ) ) continue;

				if ( allowNullRoutes && leg.getRoute() == null ) {
					// this is not so nice...
					leg.setRoute( new VehicleOnlyNetworkRoute() );
				}

				if ( !( leg.getRoute() instanceof NetworkRoute ) ) {
					throw new RuntimeException( "route for mode "+mode+" has non-network route "+leg.getRoute() );
				}

				((NetworkRoute) leg.getRoute()).setVehicleId( v );
			}
		}
	}

	private void allocate(
			final List<Plan> remainingPlans,
			final Map<Id, Id> allocation) {
		if ( remainingPlans.isEmpty() ) return;

		final Plan currentPlan = remainingPlans.get( 0 );
		final List<Plan> newRemainingPlans =
			remainingPlans.subList(
					1,
					remainingPlans.size() );

		final Id currentPersonId = currentPlan.getPerson().getId();
		final List<Id> possibleVehicles =
			new ArrayList<Id>(
					vehicleRessources.identifyVehiclesUsableForAgent(
						currentPersonId ) );
		// make sure order is deterministic
		Collections.sort( possibleVehicles );
		Collections.shuffle( possibleVehicles , random );

		// allocate the first available vehicle
		boolean foundVehicle = false;
		for ( Id v : possibleVehicles ) {
			if ( !allocation.values().contains( v ) ) {
				allocation.put( currentPersonId , v );
				foundVehicle = true;
				if ( log.isTraceEnabled() ) {
					log.trace( "found unused vehicle "+v+" for person "+currentPersonId );
				}
				break;
			}
		}

		// if all vehicles are allocated, allocate (one of) the
		// least used vehicle(s)
		if ( !foundVehicle ) {
			final Id v = findLeastUsedVehicle( allocation.values() , possibleVehicles );
			if (log.isTraceEnabled() ) {
				log.trace( "allocate used vehicle "+v+" for person "+currentPersonId );
			}
			allocation.put( currentPersonId , v );
		}

		allocate( newRemainingPlans , allocation );
	}

	private Id findLeastUsedVehicle(
			final Collection<Id> usedVehicles,
			final List<Id> possibleVehicles) {
		if ( possibleVehicles.isEmpty() ) return null;
		final Map<Id, Integer> counts = new HashMap<Id, Integer>();
		for ( Id v : possibleVehicles ) counts.put( v , 0 ); 

		for ( Id v : usedVehicles ) {
			final int c = counts.get( v );
			counts.put( v , c + 1 );
		}

		final List<Id> leastUsedVehicles = new ArrayList<Id>();
		int minUses = Integer.MAX_VALUE;

		for ( Map.Entry<Id, Integer> e : counts.entrySet() ) {
			final Id v = e.getKey();
			final int c = e.getValue();

			if ( c < minUses ) {
				minUses = c;
				leastUsedVehicles.clear();
			}

			assert c >= minUses;
			if ( c == minUses ) {
				leastUsedVehicles.add( v );
			}
		}

		if ( log.isTraceEnabled() ) {
			log.trace( "counts: "+counts );
			log.trace( "minimum usage: "+minUses );
			log.trace( "least used vehicles: "+leastUsedVehicles );
		}

		// make sure iteration order is deterministic
		Collections.sort( leastUsedVehicles );
		return leastUsedVehicles.get( random.nextInt( leastUsedVehicles.size() ) );
	}

	/**
	 * to put vehicle Id in legs without route.
	 * Try to make this the less dirty as possible: anything except vehicle-related
	 * operations throws an UnsupportedOperationException, so that we are sure this
	 * is not used for more than what it is meant to do.
	 */
	private static class VehicleOnlyNetworkRoute implements NetworkRoute {
		private Id v = null;

		// /////////////////////////////////////////////////////////////////////
		// active methods
		// /////////////////////////////////////////////////////////////////////
		@Override
		public void setVehicleId(final Id vehicleId) {
			this.v = vehicleId;
		}

		@Override
		public Id getVehicleId() {
			return v;
		}

		// /////////////////////////////////////////////////////////////////////
		// inactive methods
		// /////////////////////////////////////////////////////////////////////
		@Override
		@Deprecated
		public double getDistance() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setDistance(double distance) {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getTravelTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setTravelTime(double travelTime) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Id getStartLinkId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Id getEndLinkId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setStartLinkId(Id linkId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setEndLinkId(Id linkId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setLinkIds(Id startLinkId, List<Id> linkIds, Id endLinkId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setTravelCost(double travelCost) {
			throw new UnsupportedOperationException();
			
		}

		@Override
		public double getTravelCost() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Id> getLinkIds() {
			throw new UnsupportedOperationException();
		}

		@Override
		public NetworkRoute getSubRoute(Id fromLinkId, Id toLinkId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public VehicleOnlyNetworkRoute clone() {
			throw new UnsupportedOperationException();
		}
	}
}

