/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToPlans.java
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
package playground.thibautd.utils;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.EventsToLegs.LegHandler;

/**
 * @author thibautd
 */
public class EventsToPlans implements ActivityStartEventHandler, ActivityEndEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler {
	private final EventsToActivities eventsToActivities = new EventsToActivities();
	private final EventsToLegs eventsToLegs = new EventsToLegs();

	private boolean locked = false;
	private final Map<Id, Plan> agentsPlans = new HashMap<Id, Plan>();

	public EventsToPlans() {
		eventsToActivities.setActivityHandler(
				new ActivityHandler() {
					@Override
					public void handleActivity(
							final Id agentId,
							final Activity activity) {
						final Plan plan =
							MapUtils.getArbitraryObject(
								agentId,
								agentsPlans,
								new MapUtils.Factory<Plan>() {
									@Override
									public Plan create() {
										return new PlanImpl( new PersonImpl( agentId ) );
									}
								});
						plan.addActivity( activity );
					}
				});
		eventsToLegs.setLegHandler(
				new LegHandler() {
					@Override
					public void handleLeg(
							final Id agentId,
							final Leg leg) {
						final Plan plan =
							MapUtils.getArbitraryObject(
								agentId,
								agentsPlans,
								new MapUtils.Factory<Plan>() {
									@Override
									public Plan create() {
										return new PlanImpl( new PersonImpl( agentId ) );
									}
								});
							plan.addLeg( leg );
					}
				});

	}

	public Map<Id, Plan> getPlans() {
		if ( !locked ) {
			eventsToActivities.finish();
			locked = true;
		}
		return agentsPlans;
	}

	@Override
	public void reset(final int iteration) {
		eventsToActivities.reset( iteration );
		eventsToLegs.reset( iteration );
		agentsPlans.clear();
		locked = false;
	}

	@Override
	public void handleEvent(final AgentArrivalEvent event) {
		if ( locked ) throw new IllegalStateException();
		eventsToLegs.handleEvent( event );
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		if ( locked ) throw new IllegalStateException();
		eventsToLegs.handleEvent( event );
	}

	@Override
	public void handleEvent(final ActivityEndEvent event) {
		if ( locked ) throw new IllegalStateException();
		eventsToActivities.handleEvent( event );
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		if ( locked ) throw new IllegalStateException();
		eventsToActivities.handleEvent( event );
	}
}

