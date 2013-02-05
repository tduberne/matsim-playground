/* *********************************************************************** *
 * project: org.matsim.*
 * FixedGroupsTest.java
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
package playground.thibautd.socnetsim.replanning.grouping;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.cliquessim.config.CliquesConfigGroup;
import playground.thibautd.cliquessim.utils.JointControlerUtils;

/**
 * @author thibautd
 */
public class FixedGroupsTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testIterationOrderIsDeterministic() throws Exception {
		final String configFile = new File( utils.getPackageInputDirectory() ).getParentFile().getParentFile()+"/config.xml";
		final Config config = JointControlerUtils.createConfig( configFile );

		Collection<ReplanningGroup> previous = null;

		for (int i=0; i < 100; i++) {
			final FixedGroupsIdentifier identifier =
				FixedGroupsIdentifierFileParser.readCliquesFile(
						((CliquesConfigGroup) config.getModule(
							CliquesConfigGroup.GROUP_NAME)).getInputFile());
			// recreate each time, in case it changes iteration order
			final Population population = getPopulation( config );
			Collection<ReplanningGroup> groups = identifier.identifyGroups( population );

			if (previous != null) {
				assertIterationOrder(
						previous,
						groups);
			}
			previous = groups;
		}
	}

	private static void assertIterationOrder(
			final Collection<ReplanningGroup> expected,
			final Collection<ReplanningGroup> actual) {
		assertEquals(
				"not the same number of groups",
				expected.size(),
				actual.size());

		final Iterator<ReplanningGroup> expectedIter = expected.iterator();
		final Iterator<ReplanningGroup> actualIter = actual.iterator();

		int c = 0;
		while (expectedIter.hasNext()) {
			c++;
			final ReplanningGroup expectedGroup = expectedIter.next();
			final ReplanningGroup actualGroup = actualIter.next();

			final Iterator<Person> actualGroupIterator = actualGroup.getPersons().iterator();
			for (Person expectedPerson : expectedGroup.getPersons()) {
				assertEquals(
						"groups "+expectedGroup+" and "+actualGroup+" in position "+
						c+" are not equal or do not present the persons in the same order",
						expectedPerson.getId(),
						actualGroupIterator.next().getId());
			}
		}
	}

	private static Population getPopulation( final Config config ) {
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		// generate persons not allocated to any clique,
		// making sure that they are not added in the same order as the
		// ordering of their ids.
		final Random random = new Random( 1432 );
		for (int i=0; i < 100; i++) {
			scenario.getPopulation().addPerson(
					new PersonImpl(
						new IdImpl(
							"garbage-"+random.nextInt(999999)+"-"+i )));
		}
		return scenario.getPopulation();
	}
}

