/* *********************************************************************** *
 * project: org.matsim.*
 * HerbieTransitRouterFactory.java
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
package playground.thibautd.herbie;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.scoring.TravelScoringFunction;

import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Initialises a TransitRouterFactory, which takes into account the travel distance
 * in the same way the herbie scoring function does.
 * 
 * note: this reveal a poor separation of concerns (the travel disutility
 * implementation makes assumptions on the scoring, which can easily be broken
 * without any error)! Some way to reduce this fact should be found (use a
 * "disutilityCalculator" in both routing and scoring for example... except
 * that the would require any travel time disutility to be additive...)
 *
 * @author thibautd
 */
public class HerbieTransitRouterFactory implements TransitRouterFactory {

	private final TransitSchedule schedule;
	private final TransitRouterConfig config;
	private final TransitRouterNetwork routerNetwork;
	private final HerbieConfigGroup herbieConfig;
	private final TravelScoringFunction travelScoring;

	public HerbieTransitRouterFactory(
			final TransitSchedule schedule,
			final TransitRouterConfig config,
			final HerbieConfigGroup herbieConfig,
			final TravelScoringFunction travelScoring) {
		this.schedule = schedule;
		this.config = config;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.beelineWalkConnectionDistance);
		this.herbieConfig = herbieConfig;
		this.travelScoring = travelScoring;
	}

	@Override
	public TransitRouter createTransitRouter() {
		//TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.config);
		HerbieTransitTravelTimeAndDisutility ttCalculator =
			new HerbieTransitTravelTimeAndDisutility(
					herbieConfig,
					config,
					travelScoring);
		return new HerbieTransitRouter(
				this.config,
				this.routerNetwork,
				ttCalculator,
				ttCalculator,
				travelScoring);
	}
}
