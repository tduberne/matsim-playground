/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * {@link AbstractMultithreadedModule} using {@link JointPlanOptimizer}.
 * @author thibautd
 */
public class JointPlanOptimizerModule extends AbstractMultithreadedModule {
	private static final Logger log = Logger.getLogger(JointPlanOptimizerModule.class);

	private final JointReplanningConfigGroup configGroup;
	private final Network network;
	private final Controler controler;
	private final PersonalizableTravelCost travelCost;
	private final PersonalizableTravelTime travelTime;
	private final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;

	public JointPlanOptimizerModule(
			final Controler controler) {
		super(controler.getConfig().global());
		log.debug("JointPlanOptimizerModule constructor called with controler "+
				controler);

		// used for getting the routing algorithm
		this.controler = controler;

		this.network = controler.getScenario().getNetwork();
		this.configGroup = (JointReplanningConfigGroup)
			controler.getConfig().getModule(JointReplanningConfigGroup.GROUP_NAME);

		this.travelCost = controler.createTravelCostCalculator();
		this.travelTime = controler.getTravelTimeCalculator();

		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(
				this.network,
				controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
		controler.getEvents().addHandler(tDepDelayCalc);

		this.legTravelTimeEstimatorFactory = new JointPlanOptimizerLegTravelTimeEstimatorFactory(
				this.travelTime,
				tDepDelayCalc);
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		log.debug("JointPlanOptimizerModule.getPlanAlgoInstance called");

		PlansCalcRoute routingAlgorithm = (PlansCalcRoute) this.controler.createRoutingAlgorithm(
				this.travelCost, this.travelTime);

		return new JointPlanOptimizer(
					this.configGroup,
					controler.getScoringFunctionFactory(),
					this.legTravelTimeEstimatorFactory,
					routingAlgorithm,
					this.network,
					controler.getControlerIO().getIterationPath(
						controler.getIterationNumber())
					);
	}
}

