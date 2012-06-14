/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikerAgentFactory.java
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
package playground.thibautd.hitchiking.qsim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;

import playground.thibautd.router.TripRouter;

/**
 * @author thibautd
 */
public class HitchHikerAgentFactory implements AgentFactory {
	private final TransitAgentFactory factory;
	private final TripRouter router;
	private final PassengerQueuesManager queuesManager;
	private final EventsManager events;

	public HitchHikerAgentFactory(
			final TransitAgentFactory f,
			final TripRouter router,
			final PassengerQueuesManager queuesManager,
			final EventsManager events) {
		this.factory = f;
		this.router = router;
		this.queuesManager = queuesManager;
		this.events = events;
	}

	@Override
	public HitchHikerAgent createMobsimAgentFromPerson(final Person p) {
		return new HitchHikerAgent(
				factory.createMobsimAgentFromPerson( p ),
				router,
				queuesManager,
				events);
	}
}

