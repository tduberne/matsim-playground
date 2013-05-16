/* *********************************************************************** *
 * project: org.matsim.*
 * KtiToSimiliKtiConfig.java
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
package playground.thibautd.scripts;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import playground.meisterk.kti.config.KtiConfigGroup;

import playground.thibautd.scoring.KtiLikeScoringConfigGroup;
import playground.thibautd.utils.MyConfigUtils;

/**
 * @author thibautd
 */
public class KtiToSimiliKtiConfig {
	public static void main(final String[] args) {
		final String inputConfigFile = args[ 0 ];
		final String outputConfigFile = args[ 1 ];

		final Config inputConfig = ConfigUtils.createConfig();
		final KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
		inputConfig.addModule( KtiConfigGroup.GROUP_NAME , ktiConfigGroup );
		ConfigUtils.loadConfig( inputConfig , inputConfigFile );

		final Config outputConfig = new Config();
		final PlanCalcScoreConfigGroup planCalcScore = new PlanCalcScoreConfigGroup();
		outputConfig.addModule( PlanCalcScoreConfigGroup.GROUP_NAME , planCalcScore );
		MyConfigUtils.transmitParams( inputConfig.planCalcScore() , planCalcScore );
		planCalcScore.setConstantCar( ktiConfigGroup.getConstCar() );
		planCalcScore.setConstantBike( ktiConfigGroup.getConstBike() );
		planCalcScore.setTravelingBike_utils_hr( ktiConfigGroup.getTravelingBike() );
		planCalcScore.setMonetaryDistanceCostRatePt( (-ktiConfigGroup.getDistanceCostPtNoTravelCard() / 1000d ) / planCalcScore.getMarginalUtilityOfMoney() );
		planCalcScore.setMonetaryDistanceCostRateCar( (-ktiConfigGroup.getDistanceCostCar() / 1000d) / planCalcScore.getMarginalUtilityOfMoney() );

		final KtiLikeScoringConfigGroup ktiLikeConfigGroup = new KtiLikeScoringConfigGroup();
		outputConfig.addModule( KtiLikeScoringConfigGroup.GROUP_NAME , ktiLikeConfigGroup );
		ktiLikeConfigGroup.setTravelCardRatio( ktiConfigGroup.getDistanceCostPtUnknownTravelCard() / ktiConfigGroup.getDistanceCostPtNoTravelCard() );

		new ConfigWriter( outputConfig ).write( outputConfigFile );
	}
}

