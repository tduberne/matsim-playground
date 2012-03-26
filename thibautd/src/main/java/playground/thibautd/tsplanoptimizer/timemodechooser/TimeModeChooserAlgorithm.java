/* *********************************************************************** *
 * project: org.matsim.*
 * TimeModeChooserAlgorithm.java
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
package playground.thibautd.tsplanoptimizer.timemodechooser;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.RoutingModuleFactory;
import playground.thibautd.router.TransitRoutingModuleFactory;
import playground.thibautd.router.TripRouterFactory;
import playground.thibautd.router.controler.MultiLegRoutingControler;
import playground.thibautd.tsplanoptimizer.framework.ConfigurationBuilder;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuSearchRunner;
import playground.thibautd.tsplanoptimizer.timemodechooser.traveltimeestimation.FixedRouteNetworkRoutingModule;
import playground.thibautd.tsplanoptimizer.timemodechooser.traveltimeestimation.FixedTransitRouteRoutingModule;

/**
 * @author thibautd
 */
public class TimeModeChooserAlgorithm implements PlanAlgorithm {
	// if true, graphs of score evolution across TS iterations will
	// be created. This takes a LOT of ressources and creates a lot
	// of files. Use with care!
	private static final boolean DEBUG = false;

	private final MultiLegRoutingControler controler;
	private final TabuSearchRunner runner = new TabuSearchRunner();
	private final DepartureDelayAverageCalculator delay;

	public TimeModeChooserAlgorithm(
			final Controler controler,
			final DepartureDelayAverageCalculator delay ) {
		this.controler = (MultiLegRoutingControler) controler;
		this.delay = delay;
	}

	@Override
	public void run(final Plan plan) {
		TripRouterFactory tripRouterFactory =
			getAndTuneTripRouterFactory(
					plan,
					delay,
					controler );

		ScoringFunctionFactory scoringFunctionFactory = controler.getScoringFunctionFactory();
		ConfigurationBuilder builder =
			new TimeModeChooserConfigBuilder(
					plan,
					scoringFunctionFactory,
					tripRouterFactory,
					DEBUG ? controler.getControlerIO().getIterationPath( controler.getIterationNumber() ) : null);

		Solution bestSolution = runner.runTabuSearch( builder );

		// two goals here:
		// 1- the side effect: getRepresentedPlan sets the plan to the represented state
		// 2- the obvious check
		if (bestSolution.getRepresentedPlan() != plan) {
			throw new RuntimeException( "the returned plan is not the input plan" );
		}
	}

	// TODO: pass it to an helper class in the "estimation" package
	private static TripRouterFactory getAndTuneTripRouterFactory(
			final Plan plan,
			final DepartureDelayAverageCalculator delay,
			final MultiLegRoutingControler controler ) {
		TripRouterFactory factory = controler.getTripRouterFactory();
		
		RoutingModuleFactory moduleFactory =
			FixedRouteNetworkRoutingModule.getFactory(
					plan,
					controler.getConfig().planCalcScore(),
					controler.getPopulation().getFactory(),
					delay,
					// TODO: import from somewhere, or detect from the mobsim
					false,
					true);

		factory.setRoutingModuleFactory( TransportMode.car , moduleFactory );

		RoutingModuleFactory ptFactory = factory.getRoutingModuleFactories().get( TransportMode.pt );
		if (ptFactory instanceof TransitRoutingModuleFactory) {
			factory.setRoutingModuleFactory(
					TransportMode.pt,
					FixedTransitRouteRoutingModule.createFactory(
						plan,
						controler.getScenario().getTransitSchedule(),
						(TransitRoutingModuleFactory) ptFactory ));
		}

		return factory;
	}
}

