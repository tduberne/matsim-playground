/* *********************************************************************** *
 * project: org.matsim.*
 * PlanRouterWithVehicleRessourcesFactory.java
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
package playground.thibautd.socnetsim.sharedvehicles;

import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;

/**
 * @author thibautd
 */
public class PlanRouterWithVehicleRessourcesFactory implements PlanRoutingAlgorithmFactory {
	private final PlanRoutingAlgorithmFactory delegate;
	private final VehicleRessources ressources;

	public PlanRouterWithVehicleRessourcesFactory(
			final VehicleRessources ressources,
			final PlanRoutingAlgorithmFactory delegate) {
		this.ressources = ressources;
		this.delegate = delegate;
	}

	@Override
	public PlanAlgorithm createPlanRoutingAlgorithm(final TripRouter tripRouter) {
		return new PlanRouterWithVehicleRessources(
				ressources,
				delegate.createPlanRoutingAlgorithm(
					tripRouter ));
	}
}

