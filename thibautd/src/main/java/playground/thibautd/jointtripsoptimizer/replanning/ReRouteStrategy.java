/* *********************************************************************** *
 * project: org.matsim.*
 * ReRouteStrategy.java
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
package playground.thibautd.jointtripsoptimizer.replanning;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;

import playground.thibautd.jointtripsoptimizer.replanning.reroute.JointReRouteModule;

/**
 * {@link JointPlanStrategy} using an {@link ExpBetaPlanSelector} to select a plan
 * and uses {@link JointReRouteModule} on it.
 *
 * @author thibautd
 */
public class ReRouteStrategy extends JointPlanStrategy {

	public ReRouteStrategy(final Controler controler) {
		// TODO: use a JointPlan specific selector?
		// + pass it from the config file
		// this.planSelector = new BestPlanSelector();
		this.planSelector = new ExpBetaPlanSelector(controler.getConfig().planCalcScore());

		this.addStrategyModule(new JointReRouteModule(controler));
	}
}

