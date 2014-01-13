/* *********************************************************************** *
 * project: org.matsim.*
 * RunTRBModel.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.scripts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.initialdemandgeneration.socnetgen.framework.Agent;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.ModelRunner;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.SocialNetwork;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.SocialNetworkWriter;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.SocialPopulation;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.ThresholdFunction;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.UtilityFunction;

/**
 * @author thibautd
 */
public class RunTRBModel {
	public static void main(final String[] args) {
		final String populationFile = args[ 0 ];
		final String outputNetworkFile = args[ 1 ];
		final int stepSize = args.length > 2 ? Integer.parseInt( args[ 2 ] ) : 1;

		final SocialPopulation<ArentzeAgent> population = parsePopulation( populationFile );
		
		final ModelRunner<ArentzeAgent> runner = new ModelRunner<ArentzeAgent>();

		runner.setSamplingRate( stepSize );
		runner.setUtilityFunction(
				new UtilityFunction<ArentzeAgent>() {
					@Override
					public double calcTieUtility(
							final ArentzeAgent ego,
							final ArentzeAgent alter) {
						final int ageClassDifference = Math.abs( ego.getAgeCategory() - alter.getAgeCategory() );

						// increase distance by 1 (normally meter) to avoid linking with all agents
						// living in the same place.
						// TODO: test sensitivity of the results to this
						return -1.222 * Math.log( CoordUtils.calcDistance( ego.getCoord() , alter.getCoord() ) + 1 )
							+0.725 * dummy( ego.isMale() == alter.isMale() )
							+0.918 * dummy( ageClassDifference == 0 )
							-0.227 * dummy( ageClassDifference == 2 )
							-1.314 * dummy( ageClassDifference == 3 )
							-1.934 * dummy( ageClassDifference == 4 );
					}
				});
		runner.setThresholds( new ThresholdFunction( 1.735 , -0.1 ) );

		// TODO: iterate and calibrate thresholds
		final SocialNetwork network = runner.run( population );

		new SocialNetworkWriter().write( outputNetworkFile , network );
	}

	private static double dummy(final boolean b) {
		return b ? 1 : 0;
	}

	private static SocialPopulation<ArentzeAgent> parsePopulation(final String populationFile) {
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimPopulationReader( scenario ).parse( populationFile );

		final SocialPopulation<ArentzeAgent> population = new SocialPopulation<ArentzeAgent>();

		final Counter counter = new Counter( "convert person to agent # " );
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			// XXX this is specific to the herbie population
			if ( Integer.parseInt( person.getId().toString() ) > 1000000000 ) continue;
			counter.incCounter();

			try {
				final int age = ((PersonImpl) person).getAge();
				if ( age < 0 ) throw new IllegalArgumentException( ""+age );
				final int ageCategory = age <= 23 ? 1 : age <= 37 ? 2 : age <= 50 ? 3 : age <= 65 ? 4 : 5;
				final boolean male = ((PersonImpl) person).getSex().equals( "m" );
				final Coord coord = ((Activity) person.getSelectedPlan().getPlanElements().get( 0 )).getCoord();
				population.addAgent(
						new ArentzeAgent(
							person.getId(),
							ageCategory,
							male,
							coord));
			}
			catch (Exception e) {
				throw new RuntimeException( "exception when processing "+person , e );
			}
		}
		counter.printCounter();

		return population;
	}

	private static class ArentzeAgent implements Agent {
		private final Id id;
		private final int ageCategory;
		private final boolean isMale;
		private final Coord coord;

		public ArentzeAgent(
				final Id id,
				final int ageCategory,
				final boolean male,
				final Coord coord) {
			this.id = id;
			this.ageCategory = ageCategory;
			this.isMale = male;
			this.coord = coord;
		}

		@Override
		public Id getId() {
			return id;
		}

		public int getAgeCategory() {
			return this.ageCategory;
		}

		public boolean isMale() {
			return this.isMale;
		}

		public Coord getCoord() {
			return this.coord;
		}
	}
}
