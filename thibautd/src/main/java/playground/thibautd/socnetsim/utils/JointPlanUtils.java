/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.PassengerRoute;

/**
 * @author thibautd
 */
public class JointPlanUtils {
	private JointPlanUtils() {}

	public static JointTravelStructure analyseJointTravel(final JointPlan plan) {
		// this is a two-passes algorithm:
		// 1- parse the plans, and identify who drives who from where to where
		// 2- parse the plans to identify the corresponding passenger legs.
		// This approach is the best I could find, as it may take several driver
		// legs to drive one passenger to destination, e.g.:
		//                    p={1,2}       p={1}
		// driver ...-----|############|#############|------...
		// pass 1 ...-----|############|---------------------...
		// pass 2 ...-----|##########################|------...
		// (TD, sept. 2012)
		List<DriverTrip> driverTrips = parseDriverTrips( plan );
		List<JointTrip> jointTrips = reconstructJointTrips( driverTrips , plan );

		return new JointTravelStructure( jointTrips );
	}

	private static List<JointTrip> reconstructJointTrips(
			final List<DriverTrip> driverTrips,
			final JointPlan plan) {
		List<JointTrip> jointTrips = new ArrayList<JointTrip>();
		Map<Id, Iterator<PlanElement>> iterators = extractIterators( plan );

		for ( DriverTrip driverTrip : driverTrips ) {
			for (Map.Entry<Id, Id> e : driverTrip.passengerOrigins.entrySet()) {
				Id passengerId = e.getKey();
				Id originId = e.getValue();
				Id destinationId = driverTrip.passengerDestinations.get( passengerId );

				Iterator<PlanElement> it = iterators.get( passengerId );
				while ( it.hasNext() ) {
					PlanElement pe = it.next();
					if (pe instanceof Leg &&
							((Leg) pe).getMode().equals( JointActingTypes.PASSENGER )) {
						PassengerRoute route = (PassengerRoute) ((Leg) pe).getRoute();
						if (route.getStartLinkId().equals( originId ) &&
								route.getEndLinkId().equals( destinationId )) {
							jointTrips.add(
									new JointTrip(
										driverTrip.driverId,
										extractDriverSubTrip(
											originId,
											destinationId,
											driverTrip.driverTrip ),
										passengerId,
										(Leg) pe ));
							break;
						}
					}
				}


			}
		}

		return jointTrips;
	}

	private static List<Leg> extractDriverSubTrip(
			final Id originId,
			final Id destinationId,
			final List<Leg> driverTrip) {
		ArrayList<Leg> subTrip = new ArrayList<Leg>();
		boolean inSubTrip = false;

		for ( Leg l : driverTrip ) {
			if (l.getRoute().getStartLinkId().equals( originId )) inSubTrip = true;
			if (inSubTrip) subTrip.add( l );
			if (l.getRoute().getEndLinkId().equals( destinationId )) break;
		}

		return subTrip;
	}

	private static Map<Id, Iterator<PlanElement>> extractIterators(
			final JointPlan plan) {
		Map<Id, Iterator<PlanElement>> its = new HashMap<Id, Iterator<PlanElement>>();

		for (Plan p : plan.getIndividualPlans().values()) {
			its.put( p.getPerson().getId() , p.getPlanElements().iterator() );
		}

		return its;
	}

	/**
	 * Parses the plans and get information about driver trips, ie who drives
	 * whom from where to where.
	 * Package protected to be callable from tests
	 */
	final static List<DriverTrip> parseDriverTrips( final JointPlan plan ) {
		List<DriverTrip> driverTrips = new ArrayList<DriverTrip>();

		for ( Plan indivPlan : plan.getIndividualPlans().values() ) {
			Id driverId = indivPlan.getPerson().getId();
			List<Id> currentPassengers = new ArrayList<Id>();
			DriverTrip currentDriverTrip = null;
			for ( PlanElement pe : indivPlan.getPlanElements() ) {
				if ( pe instanceof Leg &&
						JointActingTypes.DRIVER.equals( ((Leg) pe).getMode() ) ) {
					if ( currentDriverTrip == null ) {
						currentDriverTrip = new DriverTrip( driverId );
						driverTrips.add( currentDriverTrip );
						currentPassengers.clear();
					}
					currentDriverTrip.driverTrip.add( (Leg) pe );

					DriverRoute dRoute = (DriverRoute) ((Leg) pe).getRoute();
					Id origin = dRoute.getStartLinkId();
					Id destination = dRoute.getEndLinkId();
					Collection<Id> passengerIds = dRoute.getPassengersIds();

					for ( Id passengerId : passengerIds ) {
						if ( !currentPassengers.contains( passengerId ) ) {
							currentDriverTrip.passengerOrigins.put(
									passengerId,
									origin );
							currentPassengers.add( passengerId );
						}
					}

					Iterator<Id> currPassengersIter = currentPassengers.iterator();
					while ( currPassengersIter.hasNext() ) {
						Id p = currPassengersIter.next();
						if ( !passengerIds.contains( p ) ) {
							// passenger is arrived
							currPassengersIter.remove();
						}
						else {
							// update destination
							currentDriverTrip.passengerDestinations.put(
									p,
									destination );
						}
					}
				}
				else if ( pe instanceof Leg ||
						!JointActingTypes.JOINT_STAGE_ACTS.isStageActivity(
							((Activity) pe).getType() ) ) {
					currentDriverTrip = null;
				}
			}
		}

		return driverTrips;
	}

	// package protected for tests
	static final class DriverTrip {
		final Id driverId;
		final List<Leg> driverTrip = new ArrayList<Leg>();
		final Map<Id, Id> passengerOrigins = new HashMap<Id, Id>();
		final Map<Id, Id> passengerDestinations = new HashMap<Id, Id>();

		public DriverTrip(final Id driverId) {
			this.driverId = driverId;
		}

		@Override
		public int hashCode() {
			int c = 0;

			c += driverId.hashCode();
			c += driverTrip.hashCode();
			c += passengerOrigins.hashCode();
			c += passengerDestinations.hashCode();

			return c;
		}

		@Override
		public boolean equals(final Object o) {
			if (o != null && o instanceof DriverTrip) {
				DriverTrip other = (DriverTrip) o;
				return other.driverId.equals( driverId ) &&
					other.driverTrip.equals( driverTrip ) &&
					other.passengerOrigins.equals( passengerOrigins ) &&
					other.passengerDestinations.equals( passengerDestinations );
			}
			return false;
		}

		@Override
		public String toString() {
			return "[DriverTrip: driverId="+driverId
				+", driverTrip="+driverTrip
				+", passengerOrigins="+passengerOrigins
				+", passengerDestinations="+passengerDestinations+"]";
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// Data structures
	/**
	 * Represents the "joint travel structure" of the plan, that is,
	 * who travels with whom from where to where, with references
	 * to the relevant plan elements.
	 */
	public static final class JointTravelStructure {
		private final List<JointTrip> jointTrips;

		public JointTravelStructure(
				final List<JointTrip> jointTrips) {
			this.jointTrips = jointTrips;
		}	

		public List<JointTrip> getJointTrips() {
			return jointTrips;
		}

		@Override
		public int hashCode() {
			return getJointTrips().hashCode();
		}

		@Override
		public boolean equals(final Object o) {
			if (o != null && o instanceof JointTravelStructure) {
				JointTravelStructure other = (JointTravelStructure) o;
				return other.getJointTrips().size() == getJointTrips().size() &&
					other.getJointTrips().containsAll( getJointTrips() );
			}
			return false;
		}

		@Override
		public String toString() {
			return "[JointTravelStructure: "+getJointTrips()+"]";
		}
	}

	/**
	 * Gathers information releted to one joint trip, that is,
	 * one (and <b>only one</b>) passenger being driven.
	 * Note that driver legs may pertain to several joint trips,
	 * if the driver drives several passengers at the same time.
	 */
	public static final class JointTrip {
		private final Id driverId;
		private final Id passengerId;
		private final List<Leg> driverLegs;
		private final Leg passengerLeg;

		public JointTrip(
				final Id driverId,
				final List<Leg> driverLegs,
				final Id passengerId,
				final Leg passengerLeg) {
			this.driverId = driverId;
			this.passengerId = passengerId;
			this.driverLegs = Collections.unmodifiableList( driverLegs );
			this.passengerLeg = passengerLeg;
		}

		public List<Leg> getDriverLegs() {
			return driverLegs;
		}

		public Id getDriverId() {
			return driverId;
		}

		public Id getPassengerId() {
			return passengerId;
		}

		public Leg getPassengerLeg() {
			return passengerLeg;
		}

		@Override
		public int hashCode() {
			int c = 0;

			c += getDriverId().hashCode();
			c += getPassengerId().hashCode();
			c += getDriverLegs().hashCode();
			c += getPassengerLeg().hashCode(); 

			return c;
		}

		@Override
		public boolean equals(final Object o) {
			if (o != null && o instanceof JointTrip) {
				JointTrip other = (JointTrip) o;
				return other.getDriverId().equals( getDriverId() ) &&
					other.getPassengerId().equals( getPassengerId() ) &&
					other.getDriverLegs().equals( getDriverLegs() ) &&
					other.getPassengerLeg().equals( getPassengerLeg() );
			}
			return false;
		}

		@Override
		public String toString() {
			return "[JointTrip: driver="+getDriverId()
				+", driverTrip="+getDriverLegs()
				+", passenger="+getPassengerId()
				+", passengerLeg="+getPassengerLeg()+"]";
		}
	}
}

