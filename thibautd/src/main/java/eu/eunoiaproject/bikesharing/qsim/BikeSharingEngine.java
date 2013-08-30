/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingEngine.java
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
package eu.eunoiaproject.bikesharing.qsim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.comparators.TeleportationArrivalTimeComparator;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

import eu.eunoiaproject.bikesharing.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.qsim.BikeSharingManager.BikeSharingManagerListener;
import eu.eunoiaproject.bikesharing.scenario.BikeSharingRoute;

import playground.thibautd.utils.MapUtils;

/**
 * @author thibautd
 */
public class BikeSharingEngine implements DepartureHandler, MobsimEngine {
	
	private final BikeSharingManager bikeSharingManager;
	private InternalInterface internalInterface = null;

	private final ArrivalQueue arrivalQueue = new ArrivalQueue();

	private final MapUtils.Factory<Queue<MobsimAgent>> queueFactory =
			new MapUtils.Factory<Queue<MobsimAgent>>() {
				@Override
				public Queue<MobsimAgent> create() {
					return new ArrayDeque<MobsimAgent>();
				}
			};
	private final Map<Id, Queue<MobsimAgent>> agentsWaitingForDeparturePerStation = new HashMap<Id, Queue<MobsimAgent>>();
	private final Map<Id, Queue<MobsimAgent>> agentsWaitingForArrivalPerStation = new HashMap<Id, Queue<MobsimAgent>>();

	public BikeSharingEngine( final BikeSharingManager manager ) {
		this.bikeSharingManager = manager;
		bikeSharingManager.addListener( new Listener() );
	}

	// make this private to be sure it is not added twice
	private class Listener implements BikeSharingManagerListener {
		@Override
		public void handleChange(final StatefulBikeSharingFacility facilityInNewState) {
			handleDepartures( facilityInNewState );
			handleArrivals( facilityInNewState );
		}

		public void handleDepartures(final StatefulBikeSharingFacility facilityInNewState) {
			// If there is a change in a facility, check if there are bikes for our waiting agents
			final Queue<MobsimAgent> waitingAgents = agentsWaitingForDeparturePerStation.get( facilityInNewState.getId() );
			if ( waitingAgents == null ) return;

			while ( facilityInNewState.hasBikes() && !waitingAgents.isEmpty() ) {
				final MobsimAgent agent = waitingAgents.remove();
				final boolean departed =
					handleDeparture(
						internalInterface.getMobsim().getSimTimer().getTimeOfDay(),
						agent,
						facilityInNewState.getLinkId() );
				if ( !departed ) throw new RuntimeException( "agent "+agent+" could not depart from "+facilityInNewState );
			}
		}

		public void handleArrivals(final StatefulBikeSharingFacility facilityInNewState) {
			// If there is a change in a facility, check if there are bikes for our waiting agents
			final Queue<MobsimAgent> waitingAgents = agentsWaitingForArrivalPerStation.get( facilityInNewState.getId() );
			if ( waitingAgents == null ) return;

			while ( facilityInNewState.getNumberOfBikes() < facilityInNewState.getCapacity() && !waitingAgents.isEmpty() ) {
				final MobsimAgent agent = waitingAgents.remove();
				makeAgentArrive(
						internalInterface.getMobsim().getSimTimer().getTimeOfDay(),
						agent,
						facilityInNewState );
			}
		}
	}

	@Override
	public void doSimStep(double time) {
		final Collection<MobsimAgent> agentsToArrive = arrivalQueue.retrieveAgentsForArrivalTime( time );
		for ( MobsimAgent agent : agentsToArrive ) {
			final StatefulBikeSharingFacility arrivalFacility = getArrivalFacility( agent );

			if ( arrivalFacility.getCapacity() > arrivalFacility.getNumberOfBikes() ) {
				makeAgentArrive( time , agent , arrivalFacility );
			}
			else {
				addAgentToWaitingList( agent , arrivalFacility , agentsWaitingForArrivalPerStation );
			}
		}
	}

	private void makeAgentArrive(
			final double time,
			final MobsimAgent agent,
			final StatefulBikeSharingFacility facility ) {
		agent.notifyArrivalOnLinkByNonNetworkMode(
				agent.getDestinationLinkId() );
		agent.endLegAndComputeNextState( time );
		internalInterface.arrangeNextAgentState( agent );
		bikeSharingManager.putBike( facility.getId() );
	}

	@Override
	public void onPrepareSim() { /*do nothing*/ }

	@Override
	public void afterSim() {
		final double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

		final EventsManager eventsManager =
			internalInterface.getMobsim().getEventsManager();

		for ( MobsimAgent travelingAgent : arrivalQueue.retrieveAgentsForArrivalTime( Double.POSITIVE_INFINITY ) ) {
			eventsManager.processEvent(
					eventsManager.getFactory().createAgentStuckEvent(
						now,
						travelingAgent.getId(),
						travelingAgent.getDestinationLinkId(),
						travelingAgent.getMode()));
		}

		for ( Collection<MobsimAgent> queue : agentsWaitingForDeparturePerStation.values() ) {
			for ( MobsimAgent waitingAgent : queue ) {
				eventsManager.processEvent(
						eventsManager.getFactory().createAgentStuckEvent(
							now,
							waitingAgent.getId(),
							waitingAgent.getDestinationLinkId(),
							waitingAgent.getMode()));
			}
		}

		for ( Collection<MobsimAgent> queue : agentsWaitingForArrivalPerStation.values() ) {
			for ( MobsimAgent waitingAgent : queue ) {
				eventsManager.processEvent(
						eventsManager.getFactory().createAgentStuckEvent(
							now,
							waitingAgent.getId(),
							waitingAgent.getDestinationLinkId(),
							waitingAgent.getMode()));
			}
		}
	}

	@Override
	public void setInternalInterface(final InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Override
	public boolean handleDeparture(
			final double now,
			final MobsimAgent agent,
			final Id linkId) {
		if ( !agent.getMode().equals( BikeSharingConstants.MODE ) ) return false;

		final StatefulBikeSharingFacility departureFacility = getDepartureFacility( agent );
		if ( departureFacility.hasBikes() ) {
			makeAgentDepart(
					now,
					agent );
			return true;
		}

		addAgentToWaitingList( agent , departureFacility , agentsWaitingForDeparturePerStation );

		return true;
	}

	private void addAgentToWaitingList(
			final MobsimAgent agent,
			final StatefulBikeSharingFacility departureFacility,
			final Map<Id, Queue<MobsimAgent>> queues) {
		final Queue<MobsimAgent> queue =
			MapUtils.getArbitraryObject(
					departureFacility.getId(),
					queues,
					queueFactory);
		queue.add( agent );
	}

	private void makeAgentDepart(
			final double now,
			final MobsimAgent agent ) {
		final double tt = agent.getExpectedTravelTime();
		if ( tt == Time.UNDEFINED_TIME ) {
			throw new RuntimeException( "agent "+agent+" has an undefined travel time for its bike sharing leg" );
		}

		arrivalQueue.addAgent( now + tt , agent );
		// XXX no need to fire departure event?
	}

	private StatefulBikeSharingFacility getDepartureFacility(final MobsimAgent agent) {
		final Leg leg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();
		final BikeSharingRoute route = (BikeSharingRoute) leg.getRoute();
		return bikeSharingManager.getFacilities().get( route.getOriginStation() );
	}

	private StatefulBikeSharingFacility getArrivalFacility(final MobsimAgent agent) {
		final Leg leg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();
		final BikeSharingRoute route = (BikeSharingRoute) leg.getRoute();
		return bikeSharingManager.getFacilities().get( route.getDestinationStation() );
	}
}

class ArrivalQueue {
	private Queue<Tuple<Double, MobsimAgent>> teleportationList =
		new PriorityQueue<Tuple<Double, MobsimAgent>>(
				30,
				new TeleportationArrivalTimeComparator());

	public void addAgent( final double arrivalTime , final MobsimAgent agent ) {
		teleportationList.add( new Tuple<Double, MobsimAgent>( arrivalTime , agent ) );
	}

	public Collection<MobsimAgent> retrieveAgentsForArrivalTime( final double arrivalTime ) {
		final Collection<MobsimAgent> agents = new ArrayList<MobsimAgent>();

		while ( !teleportationList.isEmpty() ) {
			final Tuple<Double, MobsimAgent> entry = teleportationList.peek();
			if (entry.getFirst().doubleValue() > arrivalTime ) return agents;

			teleportationList.poll();
			agents.add( entry.getSecond() );
		}

		return agents;
	}
}