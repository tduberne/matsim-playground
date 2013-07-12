/* *********************************************************************** *
 * project: org.matsim.*
 * AllocateVehicleToSubtourModule.java
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

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;

/**
 * @author thibautd
 */
public class AllocateVehicleToSubtourModule extends AbstractMultithreadedModule {
	private final String mode;
	private final VehicleRessources ressources;

	public AllocateVehicleToSubtourModule(
			final int nThreads,
			final String mode,
			final VehicleRessources ressources) {
		super( nThreads );
		this.mode = mode;
		this.ressources = ressources;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new AllocateVehicleToSubtourAlgorithm(
				MatsimRandom.getLocalInstance(),
				mode,
				getReplanningContext().getTripRouter(),
				ressources);
	}
}

