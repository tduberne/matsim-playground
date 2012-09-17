/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingInsertionModule.java
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
package playground.thibautd.hitchiking.replanning;

import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.controler.MultiLegRoutingControler;

/**
 * @author thibautd
 */
public class HitchHikingInsertionModule extends AbstractMultithreadedModule {
	private final MultiLegRoutingControler controler;

	public HitchHikingInsertionModule(
			final Controler controler) {
		super( controler.getConfig().global() );
		this.controler = (MultiLegRoutingControler) controler;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		TripRouterFactory factory = controler.getTripRouterFactory();

		TripRouter tripRouter = factory.createTripRouter();

		return new HitchHikingInsertionAlgorithm(
				MatsimRandom.getLocalInstance(),
				tripRouter);
	}
}

