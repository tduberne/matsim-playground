/* *********************************************************************** *
 * project: org.matsim.*
 * DiluteSocialNetworkedPopulation.java
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
package playground.thibautd.initialdemandgeneration.transformation;

import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.ivt.utils.ArgParser;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.population.SocialNetworkReader;
import playground.thibautd.socnetsim.population.SocialNetworkWriter;
import playground.thibautd.utils.MoreIOUtils;

/**
 * Given a population for a wide area (say Switzerland), goes through a dilution
 * process to restrict the scenario to an area of interest (say Zurich).
 * It does NOT sample the population, it just removes agents considered out
 * of the area of interest. See {@link SocialNetworkedPopulationDilutionUtils}
 * for more details on which agents are kept or not.
 * @author thibautd
 */
public class DiluteSocialNetworkedPopulation {
	private static enum DilutionType {
		area_only,
		area_leisure_alters,
		area_all_alters;
	}

	private static void main(final ArgParser args) throws IOException {
		args.setDefaultValue( "--xcenter" , "683518.0" );
		args.setDefaultValue( "--ycenter" , "246836.0" );

		args.setDefaultValue( "--radius" , "30000" );

		args.setDefaultValue( "--dilution-type" , "area_only" );

		args.setDefaultValue( "--netfile" , null ); // unused.
		args.setDefaultValue( "--inpopfile" , null );
		args.setDefaultValue( "--insocnet" , null );
		args.setDefaultValue( "--outdir" , null );

		final Coord center =
			new CoordImpl(
					Double.parseDouble(
						args.getValue( "--xcenter" ) ),
					Double.parseDouble(
						args.getValue( "--ycenter" ) ) );

		final double radius =
			Double.parseDouble(
					args.getValue(
						"--radius" ) );

		final DilutionType dilutionType = args.getEnumValue( "--dilution-type" , DilutionType.class );

		final String inpopfile = args.getValue( "--inpopfile" );
		final String insocnet = args.getValue( "--insocnet" );
		final String outdir = args.getValue( "--outdir" );

		MoreIOUtils.initOut( outdir );

		try {
			final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

			new MatsimPopulationReader( scenario ).readFile( inpopfile );
			new SocialNetworkReader( scenario ).parse( insocnet );

			switch ( dilutionType ) {
				case area_all_alters:
					SocialNetworkedPopulationDilutionUtils.dilute(
							scenario,
							center,
							radius );
					break;
				case area_leisure_alters:
					SocialNetworkedPopulationDilutionUtils.diluteLeisureOnly(
							scenario,
							center,
							radius );
					break;
				case area_only:
					SocialNetworkedPopulationDilutionUtils.diluteAreaOnly(
							scenario,
							center,
							radius );
					break;
				default:
					throw new RuntimeException( ""+dilutionType );
			}

			final String outpopfile = outdir+"/diluted-population.xml.gz";
			final String outsocnet = outdir+"/diluted-socialnetwork.xml.gz";

			new PopulationWriter(
					scenario.getPopulation(),
					scenario.getNetwork() ).write( outpopfile );

			new SocialNetworkWriter(
					(SocialNetwork)
						scenario.getScenarioElement(
							SocialNetwork.ELEMENT_NAME ) ).write( outsocnet );
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}

	public static void main(final String[] args) throws IOException {
		main( new ArgParser( args ) );
	}
}

