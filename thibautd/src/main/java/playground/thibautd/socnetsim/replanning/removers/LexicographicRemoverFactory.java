/* *********************************************************************** *
 * project: org.matsim.*
 * LexicographicRemoverFactory.java
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
package playground.thibautd.socnetsim.replanning.removers;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.replanning.ExtraPlanRemover;
import playground.thibautd.socnetsim.replanning.ExtraPlanRemoverFactory;

/**
 * @author thibautd
 */
public class LexicographicRemoverFactory implements ExtraPlanRemoverFactory {

	@Override
	public ExtraPlanRemover createRemover(
			final ControllerRegistry registry) {
		final GroupReplanningConfigGroup conf = (GroupReplanningConfigGroup)
				registry.getScenario().getConfig().getModule(
						GroupReplanningConfigGroup.GROUP_NAME );

		return new LexicographicForCompositionExtraPlanRemover(
				conf.getMaxPlansPerComposition(),
				conf.getMaxPlansPerAgent() );
	}
}

