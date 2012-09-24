/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateEquilibriumOptimalPlans.java
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
package playground.thibautd.analysis.aposteriorianalysis;

import playground.thibautd.cliquessim.run.JointControler;
import playground.thibautd.cliquessim.utils.JointControlerUtils;
import playground.thibautd.utils.MoreIOUtils;

/**
 * @author thibautd
 */
public class GenerateEquilibriumOptimalPlans {
	public static void main(final String[] args) {
		String configFile = args[ 0 ];
		String outputDir = args[ 1 ];

		MoreIOUtils.initOut( outputDir );

		JointControler controler = (JointControler) JointControlerUtils.createControler( configFile );
		controler.run();

		EquilibriumOptimalPlansGenerator generator =
			new EquilibriumOptimalPlansGenerator( controler );

		generator.writePopulations( outputDir );
	}
}

