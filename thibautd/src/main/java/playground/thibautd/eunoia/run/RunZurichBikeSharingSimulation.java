/* *********************************************************************** *
 * project: org.matsim.*
 * RunZurichBikeSharingSimulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.eunoia.run;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.experimental.ReflectiveConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.ivt.matsim2030.Matsim2030Utils;
import playground.thibautd.eunoia.scoring.Matsim2010BikeSharingScoringFunctionFactory;
import playground.thibautd.router.CachingRoutingModuleWrapper;
import playground.thibautd.router.multimodal.CachingLeastCostPathAlgorithmWrapper;
import playground.thibautd.utils.SoftCache;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.qsim.BikeSharingWithoutRelocationQsimFactory;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingScenarioUtils;
import eu.eunoiaproject.bikesharing.scoring.StepBasedFareConfigGroup;

/**
 * @author thibautd
 */
public class RunZurichBikeSharingSimulation {
	private static final Logger log =
		Logger.getLogger(RunZurichBikeSharingSimulation.class);

	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		OutputDirectoryLogging.catchLogEntries();
		Logger.getLogger( SoftCache.class ).setLevel( Level.TRACE );

		final Config config = BikeSharingScenarioUtils.loadConfig( configFile );
		final DenivelationConfigGroup denivelationConfig = new DenivelationConfigGroup();
		Matsim2030Utils.addDefaultGroups( config );
		config.addModule( new StepBasedFareConfigGroup() );
		config.addModule( new MultiModalConfigGroup() );
		config.addModule( denivelationConfig );

		failIfExists( config.controler().getOutputDirectory() );

		if ( !config.planCalcScore().getModes().containsKey( BikeSharingConstants.MODE ) ) {
			log.warn( "adding the disutility of bike sharing programmatically!" );
			final ModeParams bikesharing = config.planCalcScore().getOrCreateModeParams( BikeSharingConstants.MODE );
			final ModeParams bike = config.planCalcScore().getOrCreateModeParams( TransportMode.bike );

			bikesharing.setConstant( bike.getConstant() );
			bikesharing.setMarginalUtilityOfDistance( bike.getMarginalUtilityOfDistance() );
			bikesharing.setMarginalUtilityOfTraveling( bike.getMarginalUtilityOfTraveling() );
			bikesharing.setMonetaryDistanceCostRate( bike.getMonetaryDistanceCostRate() );
		}

		final RelocationConfigGroup relocationGroup = new RelocationConfigGroup();
		config.addModule( relocationGroup );

		final Scenario sc = ScenarioUtils.createScenario( config );
		BikeSharingScenarioUtils.configurePopulationFactory( sc );
		Matsim2030Utils.loadScenario( sc );
		BikeSharingScenarioUtils.loadBikeSharingPart( sc );

		final Controler controler = new Controler( sc );

		controler.addControlerListener(
				new ShutdownListener() {
					@Override
					public void notifyShutdown(final ShutdownEvent event) {
						CachingRoutingModuleWrapper.logStats();
						CachingLeastCostPathAlgorithmWrapper.logStats();
					}
				});

		// Not sure how well (or bad) this would interact with Bike Sharing...
		// I expect pretty nasty stuff to silently happen.
		// Matsim2030Utils.initializeLocationChoice( controler );

		final double refBikeSpeed = sc.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.bike);
		final double utilGain_m =
					sc.getConfig().planCalcScore().getModes().get( TransportMode.bike ).getMarginalUtilityOfTraveling() * 
						(denivelationConfig.getEquivalentDistanceForAltitudeGain() /
						refBikeSpeed);

		controler.setTripRouterFactory(
				BikeSharingScenarioUtils.createTripRouterFactoryAndConfigureRouteFactories(
					controler.getTravelDisutilityFactory(),
					controler.getScenario(),
					utilGain_m ) );

		switch ( relocationGroup.getStrategy() ) {
		case noRelocation:
			controler.setMobsimFactory( new BikeSharingWithoutRelocationQsimFactory( false ) );
			break;
		case systemWideCapacities:
			controler.setMobsimFactory( new BikeSharingWithoutRelocationQsimFactory( true ) );
			break;
		default:
			throw new RuntimeException();
		}

		controler.setScoringFunctionFactory(
				new Matsim2010BikeSharingScoringFunctionFactory(
					sc,
					utilGain_m ) );
						
		Matsim2030Utils.loadControlerListeners( controler );

		controler.run();
	}

	private static void failIfExists(final String outdir) {
		final File file = new File( outdir +"/" );
		if ( file.exists() && file.list().length > 0 ) {
			throw new UncheckedIOException( "Directory "+outdir+" exists and is not empty!" );
		}
	}

	private static class RelocationConfigGroup extends ReflectiveConfigGroup {
		public static final String GROUP_NAME = "bikeSharingRedistribution";

		public static enum Strategy {
			noRelocation, systemWideCapacities;
		}
		private Strategy strategy = Strategy.systemWideCapacities;

		public RelocationConfigGroup() {
			super( GROUP_NAME );
		}

		@StringGetter( "strategy" )
		public Strategy getStrategy() {
			return this.strategy;
		}

		@StringSetter( "strategy" )
		public void setStrategy(Strategy strategy) {
			this.strategy = strategy;
		}
	}

	private static class DenivelationConfigGroup extends ReflectiveConfigGroup {
		public static final String GROUP_NAME = "denivelationScoring";

		// from Gregory Erhardt: biking up 1 distance unit is equivalent to
		// biking additional 31.68 units
		private double equivalentDistanceForAltitudeGain = 31.68;

		public DenivelationConfigGroup( ) {
			super( GROUP_NAME );
		}

		@StringGetter( "equivalentDistanceForAltitudeGain" )
		public double getEquivalentDistanceForAltitudeGain() {
			return equivalentDistanceForAltitudeGain;
		}

		@StringSetter( "equivalentDistanceForAltitudeGain" )
		public void setEquivalentDistanceForAltitudeGain( double equivalentDistanceForAltitudeGain ) {
			this.equivalentDistanceForAltitudeGain = equivalentDistanceForAltitudeGain;
		}
	}
}

