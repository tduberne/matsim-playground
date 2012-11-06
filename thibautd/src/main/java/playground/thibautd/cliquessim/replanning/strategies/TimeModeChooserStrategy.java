/* *********************************************************************** *
 * project: org.matsim.*
 * TimeModeChooserStrategy.java
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
package playground.thibautd.cliquessim.replanning.strategies;

import org.matsim.core.controler.Controler;

import playground.thibautd.cliquessim.replanning.JointPlanStrategy;
import playground.thibautd.cliquessim.replanning.modules.jointtimemodechooser.JointTimeModeChooserModule;
import playground.thibautd.cliquessim.replanning.selectors.RandomPlanSelectorWithoutCasts;

/**
 * @author thibautd
 */
public class TimeModeChooserStrategy extends JointPlanStrategy {

	public TimeModeChooserStrategy(final Controler controler) {
		// selector: should be gotten from some config group.
		//super( new PlanWithLongestTypeSelector() );
		//super( new ExpBetaPlanSelector( controler.getConfig().planCalcScore() ) );
		super( new RandomPlanSelectorWithoutCasts() );

		this.addStrategyModule(new JointTimeModeChooserModule(controler));
	}
}
