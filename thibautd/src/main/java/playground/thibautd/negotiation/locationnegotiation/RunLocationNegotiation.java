/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.negotiation.locationnegotiation;

import com.google.inject.Key;
import org.matsim.contrib.socnetsim.framework.SocialNetworkConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Injector;
import playground.ivt.utils.MonitoringUtils;
import playground.ivt.utils.MoreIOUtils;
import playground.thibautd.negotiation.framework.NegotiationScenarioFromFileModule;
import playground.thibautd.negotiation.framework.Negotiator;
import playground.thibautd.negotiation.framework.NegotiatorConfigGroup;

import java.io.IOException;

/**
 * @author thibautd
 */
public class RunLocationNegotiation {
	public static void main( final String... args ) throws Exception {
		final Config config =
				ConfigUtils.loadConfig(
						args[ 0 ] ,
						new NegotiatorConfigGroup() ,
						new LocationUtilityConfigGroup() ,
						new SocialNetworkConfigGroup() );

		try ( AutoCloseable out = MoreIOUtils.initOut( config ) ;
			  AutoCloseable monitor = MonitoringUtils.monitorAndLogOnClose();
			  AutoCloseable writingMonitor = MonitoringUtils.writeGCFigure( config.controler().getOutputDirectory()+"/gc.dat" )) {
			run( config );
		}
	}

	private static void run( final Config config ) throws IOException {
		final Negotiator<LocationProposition> negotiator =
				Injector.createInjector(
						config,
						new NegotiationScenarioFromFileModule( LocationProposition.class ),
						new LocationNegotiationModule() ).getInstance(
								new Key<Negotiator<LocationProposition>>() {} );

		try ( ChosenLocationWriter writer = new ChosenLocationWriter( config.controler().getOutputDirectory()+"/locations.dat" ) ) {
			negotiator.negotiate( writer::writeLocation );
		}
	}


}
