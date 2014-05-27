/* *********************************************************************** *
 * project: org.matsim.*
 * RunMatsim2010SocialScenario.java
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
package playground.thibautd.socnetsim.run;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.experimental.ReflectiveModule;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

import playground.ivt.matsim2030.generation.ScenarioMergingConfigGroup;
import playground.ivt.matsim2030.Matsim2030Utils;
import playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory;

import playground.thibautd.config.NonFlatConfigReader;
import playground.thibautd.config.NonFlatConfigWriter;
import playground.thibautd.initialdemandgeneration.transformation.SocialNetworkedPopulationDilutionUtils;
import playground.thibautd.initialdemandgeneration.transformation.SocialNetworkedPopulationDilutionUtils.DilutionType;
import playground.thibautd.scoring.KtiScoringFunctionFactoryWithJointModes;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ControllerRegistryBuilder;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.population.SocialNetworkReader;
import playground.thibautd.socnetsim.replanning.grouping.DynamicGroupIdentifier;
import playground.thibautd.socnetsim.SocialNetworkConfigGroup;
import playground.thibautd.socnetsim.utils.JointScenarioUtils;

/**
 * @author thibautd
 */
public class RunMatsim2010SocialScenario {
	private static final Logger log =
		Logger.getLogger(RunMatsim2010SocialScenario.class);

	public static void main(final String[] args) {
		OutputDirectoryLogging.catchLogEntries();
		final String configFile = args[ 0 ];

		final Config config = loadConfig(configFile);
		final Scenario scenario = loadScenario(config);

		final ImmutableJointController controller = createController( scenario );

		RunUtils.loadBeingTogetherListenner( controller );

		final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup)
				config.getModule( GroupReplanningConfigGroup.GROUP_NAME );
		
		RunUtils.loadDefaultAnalysis(
				weights.getGraphWriteInterval(),
				null , // cliques...
				controller );

		if ( weights.getCheckConsistency() ) {
			// those listenners check the coordination behavior:
			// do not ad if not used
			RunUtils.addConsistencyCheckingListeners( controller );
		}
		RunUtils.addDistanceFillerListener( controller );

		try { 
			// run it
			controller.run();
		}
		finally {
			// dump non flat config
			new NonFlatConfigWriter( config ).write( controller.getControlerIO().getOutputFilename( "output_config.xml.gz" ) );
		}

	}

	private static ImmutableJointController createController(
			final Scenario scenario) {
		final Config config = scenario.getConfig();
		if ( ((ScoringFunctionConfigGroup) config.getModule( ScoringFunctionConfigGroup.GROUP_NAME )).isUseKtiScoring() ) {
			log.warn( "the parameter \"useKtiScoring\" from module "+ScoringFunctionConfigGroup.GROUP_NAME+" will be set to false" );
			log.warn( "a KTI-like scoring is already set from the script." );
			((ScoringFunctionConfigGroup) config.getModule( ScoringFunctionConfigGroup.GROUP_NAME )).setUseKtiScoring( false );
		}
		final ControllerRegistry controllerRegistry =
			RunUtils.loadDefaultRegistryBuilder(
				new ControllerRegistryBuilder( scenario )
					.withGroupIdentifier( 
							new DynamicGroupIdentifier(
								scenario ) )
					.withScoringFunctionFactory(
							new KtiScoringFunctionFactoryWithJointModes(
								new MATSim2010ScoringFunctionFactory(
									scenario,
									new StageActivityTypesImpl(
										PtConstants.TRANSIT_ACTIVITY_TYPE,
										JointActingTypes.INTERACTION) ),
								scenario )),
				scenario ).build();

		final ImmutableJointController controller = RunUtils.initializeController( controllerRegistry );
		return controller;
	}

	private static Scenario loadScenario(final Config config) {
		final Scenario scenario = JointScenarioUtils.loadScenario( config );
		RunUtils.enrichScenario( scenario );
		Matsim2030Utils.enrichScenario( scenario );
		scenario.getConfig().controler().setCreateGraphs( false ); // cannot set that from config file...

		final SocialNetworkConfigGroup snConf = (SocialNetworkConfigGroup)
				config.getModule( SocialNetworkConfigGroup.GROUP_NAME );

		new SocialNetworkReader( scenario ).parse( snConf.getInputFile() );

		final SocialNetwork sn = (SocialNetwork) scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		for ( Id p : scenario.getPopulation().getPersons().keySet() ) {
			if ( !sn.getEgos().contains( p ) ) sn.addEgo( p );
		}

		final ScenarioMergingConfigGroup mergingGroup = (ScenarioMergingConfigGroup)
			config.getModule( ScenarioMergingConfigGroup.GROUP_NAME );
		if ( mergingGroup.getPerformDilution() ) {
			final SocialDilutionConfigGroup dilutionConfig = (SocialDilutionConfigGroup)
				config.getModule( SocialDilutionConfigGroup.GROUP_NAME );
			log.info( "performing \"dilution\" with method "+dilutionConfig.getDilutionType() );
			SocialNetworkedPopulationDilutionUtils.dilute(
					dilutionConfig.getDilutionType(),
					scenario,
					mergingGroup.getDilutionCenter(),
					mergingGroup.getDilutionRadiusM() );
		}
		return scenario;
	}

	private static Config loadConfig(final String configFile) {
		final Config config = ConfigUtils.createConfig();
		JointScenarioUtils.addConfigGroups( config );
		RunUtils.addConfigGroups( config );
		// some redundancy here... just add scenarioMerging "by hand"
		//Matsim2030Utils.addDefaultGroups( config );
		config.addModule( new ScenarioMergingConfigGroup() );
		config.addModule( new SocialDilutionConfigGroup() );
		new NonFlatConfigReader( config ).parse( configFile );
		return config;
	}

	private static class SocialDilutionConfigGroup extends ReflectiveModule {
		public static final String GROUP_NAME = "socialDilution";

		// this is the most efficient (it removes the most agents)
		private DilutionType dilutionType = DilutionType.areaOnly;

		public SocialDilutionConfigGroup() {
			super( GROUP_NAME );
		}

		@StringGetter( "dilutionType" )
		public DilutionType getDilutionType() {
			return this.dilutionType;
		}

		@StringSetter( "dilutionType" )
		public void setDilutionType(DilutionType dilutionType) {
			this.dilutionType = dilutionType;
		}
	}
}
