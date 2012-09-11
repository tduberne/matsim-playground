/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingControler.java
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
package playground.thibautd.hitchiking.run;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.thibautd.hitchiking.HitchHikingUtils;
import playground.thibautd.hitchiking.qsim.HitchHikingQsimFactory;
import playground.thibautd.hitchiking.routing.HitchHikingTripRouterFactory;
import playground.thibautd.hitchiking.spotweights.SpotWeighter;
import playground.thibautd.router.controler.MultiLegRoutingControler;
import playground.thibautd.router.RoutingElements;

/**
 * @author thibautd
 */
public class HitchHikingControler extends MultiLegRoutingControler {
	private final SpotWeighter spotWeighter;

	@Override
	protected void loadControlerListeners() {
		addControlerListener( new StartupListener() {
			@Override
			public void notifyStartup(final StartupEvent event) {
				setTripRouterFactory(
					new HitchHikingTripRouterFactory(
						new RoutingElements( event.getControler() ),
						HitchHikingUtils.getSpots( getScenario() ),
						spotWeighter,
						HitchHikingUtils.getConfigGroup( getConfig() )));
			}
		});

		super.loadControlerListeners();
	}


	public HitchHikingControler(
			final Scenario scenario,
			final SpotWeighter spotWeighter) {
		super(scenario);
		this.spotWeighter = spotWeighter;
	}

	@Override
	protected void loadData() {
		setMobsimFactory( new HitchHikingQsimFactory( this ) );
		super.loadData();
	}

}

