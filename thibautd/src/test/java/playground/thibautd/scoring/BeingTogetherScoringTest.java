/* *********************************************************************** *
 * project: org.matsim.*
 * BeingTogetherScoringTest.java
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
package playground.thibautd.scoring;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.scoring.BeingTogetherScoring.AcceptAllFilter;
import playground.thibautd.scoring.BeingTogetherScoring.RejectAllFilter;

/**
 * @author thibautd
 */
public class BeingTogetherScoringTest {

	@Test
	public void testOvelapsOfActivities() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";
		
		for ( OverlapSpec os : new OverlapSpec[]{
				new OverlapSpec( 0 , 10 , 20 , 30 ),
				new OverlapSpec( 0 , 10 , 10 , 30 ),
				new OverlapSpec( 0 , 20 , 10 , 30 ),
				new OverlapSpec( 10 , 20 , 10 , 30 ),
				new OverlapSpec( 20 , 30 , 10 , 30 ),
				new OverlapSpec( 30 , 50 , 10 , 30 ) }) {
			final BeingTogetherScoring testee =
				new BeingTogetherScoring(
						null, // facilities
						1,
						ego,
						Collections.singleton( alter ) );
			testee.handleEvent(
					new ActivityStartEvent(os.startEgo, ego, linkId, null, type) );
			testee.handleEvent(
					new ActivityStartEvent(os.startAlter, alter, linkId, null, type) );
			testee.handleEvent(
					new ActivityEndEvent(os.endEgo, ego, linkId, null, type) );
			testee.handleEvent(
					new ActivityEndEvent(os.endAlter, alter, linkId, null, type) );

			Assert.assertEquals(
					"unexpected overlap for "+os,
					Math.max( Math.min( os.endAlter , os.endEgo ) - Math.max( os.startAlter , os.startEgo ) , 0 ),
					testee.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testWrapAround() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";

		final BeingTogetherScoring testee =
			new BeingTogetherScoring(
					null, // facilities
					1,
					ego,
					Collections.singleton( alter ) );

		testee.handleEvent(
				new ActivityEndEvent(10, ego, linkId, null, type) );
		testee.handleEvent(
				new ActivityStartEvent(10, ego, linkId, null, type) );

		testee.handleEvent(
				new ActivityEndEvent(100, alter, linkId, null, type) );
		testee.handleEvent(
				new ActivityStartEvent(100, alter, linkId, null, type) );

		Assert.assertEquals(
				"unexpected overlap",
				24 * 3600,
				testee.getScore(),
				MatsimTestUtils.EPSILON);
	}

	@Test
	public void testWrapAroundAfter24h() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";

		final BeingTogetherScoring testee =
			new BeingTogetherScoring(
					null, // facilities
					1,
					ego,
					Collections.singleton( alter ) );

		testee.handleEvent(
				new ActivityEndEvent(10, ego, linkId, null, type) );
		testee.handleEvent(
				new ActivityStartEvent(10, ego, linkId, null, type) );

		testee.handleEvent(
				new ActivityEndEvent(26 * 3600, alter, linkId, null, type) );
		testee.handleEvent(
				new ActivityStartEvent(26 * 3600, alter, linkId, null, type) );

		Assert.assertEquals(
				"unexpected overlap",
				24 * 3600,
				testee.getScore(),
				MatsimTestUtils.EPSILON);
	}

	@Test
	public void testNoOverlapIfDifferentActTypes() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";
		final String type2 = "type2";

		final BeingTogetherScoring testee =
			new BeingTogetherScoring(
					null, // facilities
					1,
					ego,
					Collections.singleton( alter ) );

		testee.handleEvent(
				new ActivityStartEvent(0, ego, linkId, null, type) );
		testee.handleEvent(
				new ActivityStartEvent(0, alter, linkId, null, type2) );
		testee.handleEvent(
				new ActivityEndEvent(100, ego, linkId, null, type) );
		testee.handleEvent(
				new ActivityEndEvent(100, alter, linkId, null, type2) );

		Assert.assertEquals(
				"unexpected overlap",
				0,
				testee.getScore(),
				MatsimTestUtils.EPSILON);

	}

	@Test
	public void testNoOverlapIfDifferentLocations() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final Id linkId2 = new IdImpl( 2 );
		final String type = "type";

		final BeingTogetherScoring testee =
			new BeingTogetherScoring(
					null, // facilities
					1,
					ego,
					Collections.singleton( alter ) );

		testee.handleEvent(
				new ActivityStartEvent(0, ego, linkId, null, type) );
		testee.handleEvent(
				new ActivityStartEvent(0, alter, linkId2, null, type) );
		testee.handleEvent(
				new ActivityEndEvent(100, ego, linkId, null, type) );
		testee.handleEvent(
				new ActivityEndEvent(100, alter, linkId2, null, type) );

		Assert.assertEquals(
				"unexpected overlap",
				0,
				testee.getScore(),
				MatsimTestUtils.EPSILON);

	}

	@Test
	public void testNoOverlapIfRejectedActivity() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";

		final BeingTogetherScoring testee =
			new BeingTogetherScoring(
					null, // facilities
					new RejectAllFilter(),
					new AcceptAllFilter(),
					1,
					ego,
					Collections.singleton( alter ) );

		testee.handleEvent(
				new ActivityStartEvent(0, ego, linkId, null, type) );
		testee.handleEvent(
				new ActivityStartEvent(0, alter, linkId, null, type) );
		testee.handleEvent(
				new ActivityEndEvent(100, ego, linkId, null, type) );
		testee.handleEvent(
				new ActivityEndEvent(100, alter, linkId, null, type) );

		Assert.assertEquals(
				"unexpected overlap",
				0,
				testee.getScore(),
				MatsimTestUtils.EPSILON);

	}

	@Test
	public void testNoOverlapIfPlanDoesNotComplete() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type1 = "type1";
		final String type2 = "type2";

		final BeingTogetherScoring testee =
			new BeingTogetherScoring(
					null, // facilities
					new AcceptAllFilter(),
					new AcceptAllFilter(),
					1,
					ego,
					Collections.singleton( alter ) );

		// ego: go from 1 to 2 and do not complete
		testee.handleEvent(
				new ActivityEndEvent(10, ego, linkId, null, type1) );
		testee.handleEvent(
				new ActivityStartEvent(10, ego, linkId, null, type2) );

		// alter: 1 to 2 back to 1.
		testee.handleEvent(
				new ActivityEndEvent(20, alter, linkId, null, type1) );
		testee.handleEvent(
				new ActivityStartEvent(20, alter, linkId, null, type2) );
		testee.handleEvent(
				new ActivityEndEvent(30, alter, linkId, null, type2) );
		testee.handleEvent(
				new ActivityStartEvent(30, alter, linkId, null, type1) );

		// Two behaviors would be valid:
		// - no overlap (consider undefined)
		// - overlap for type 2 (consider ego performs 2 until the end of times)
		// Here, consider no overlap (this seems the safest)
		Assert.assertEquals(
				"unexpected overlap",
				0,
				testee.getScore(),
				MatsimTestUtils.EPSILON);

	}


	@Test
	public void testNoOverlapIfWrongAgent() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		final Id other = new IdImpl( "tonny montana" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";

		final BeingTogetherScoring testee =
			new BeingTogetherScoring(
					null, // facilities
					new RejectAllFilter(),
					new AcceptAllFilter(),
					1,
					ego,
					Collections.singleton( alter ) );

		testee.handleEvent(
				new ActivityStartEvent(0, ego, linkId, null, type) );
		testee.handleEvent(
				new ActivityStartEvent(0, other, linkId, null, type) );
		testee.handleEvent(
				new ActivityEndEvent(100, ego, linkId, null, type) );
		testee.handleEvent(
				new ActivityEndEvent(100, other, linkId, null, type) );

		Assert.assertEquals(
				"unexpected overlap",
				0,
				testee.getScore(),
				MatsimTestUtils.EPSILON);

	}

	@Test
	public void testOvelapsOfLegs() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id vehId = new IdImpl( 1 );
		
		for ( OverlapSpec os : new OverlapSpec[]{
				new OverlapSpec( 0 , 10 , 20 , 30 ),
				new OverlapSpec( 0 , 10 , 10 , 30 ),
				new OverlapSpec( 0 , 20 , 10 , 30 ),
				new OverlapSpec( 10 , 20 , 10 , 30 ),
				new OverlapSpec( 20 , 30 , 10 , 30 ),
				new OverlapSpec( 30 , 50 , 10 , 30 ) }) {
			final BeingTogetherScoring testee =
				new BeingTogetherScoring(
						null, // facilities
						1,
						ego,
						Collections.singleton( alter ) );
			testee.handleEvent(
					new PersonEntersVehicleEvent(os.startEgo, ego, vehId) );
			testee.handleEvent(
					new PersonEntersVehicleEvent(os.startAlter, alter, vehId) );
			testee.handleEvent(
					new PersonLeavesVehicleEvent(os.endEgo, ego, vehId) );
			testee.handleEvent(
					new PersonLeavesVehicleEvent(os.endAlter, alter, vehId) );

			Assert.assertEquals(
					"unexpected overlap for "+os,
					Math.max( Math.min( os.endAlter , os.endEgo ) - Math.max( os.startAlter , os.startEgo ) , 0 ),
					testee.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testNoOvelapIfDifferentVehicles() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id vehId = new IdImpl( 1 );
		final Id vehId2 = new IdImpl( 2 );
		
		for ( OverlapSpec os : new OverlapSpec[]{
				new OverlapSpec( 0 , 10 , 20 , 30 ),
				new OverlapSpec( 0 , 10 , 10 , 30 ),
				new OverlapSpec( 0 , 20 , 10 , 30 ),
				new OverlapSpec( 10 , 20 , 10 , 30 ),
				new OverlapSpec( 20 , 30 , 10 , 30 ),
				new OverlapSpec( 30 , 50 , 10 , 30 ) }) {
			final BeingTogetherScoring testee =
				new BeingTogetherScoring(
						null, // facilities
						1,
						ego,
						Collections.singleton( alter ) );
			testee.handleEvent(
					new PersonEntersVehicleEvent(os.startEgo, ego, vehId) );
			testee.handleEvent(
					new PersonEntersVehicleEvent(os.startAlter, alter, vehId2) );
			testee.handleEvent(
					new PersonLeavesVehicleEvent(os.endEgo, ego, vehId) );
			testee.handleEvent(
					new PersonLeavesVehicleEvent(os.endAlter, alter, vehId2) );

			Assert.assertEquals(
					"unexpected overlap for "+os,
					0,
					testee.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testNoOvelapIfRejectedMode() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id vehId = new IdImpl( 1 );
		
		for ( OverlapSpec os : new OverlapSpec[]{
				new OverlapSpec( 0 , 10 , 20 , 30 ),
				new OverlapSpec( 0 , 10 , 10 , 30 ),
				new OverlapSpec( 0 , 20 , 10 , 30 ),
				new OverlapSpec( 10 , 20 , 10 , 30 ),
				new OverlapSpec( 20 , 30 , 10 , 30 ),
				new OverlapSpec( 30 , 50 , 10 , 30 ) }) {
			final BeingTogetherScoring testee =
				new BeingTogetherScoring(
						null, // facilities
						new AcceptAllFilter(),
						new RejectAllFilter(),
						1,
						ego,
						Collections.singleton( alter ) );
			testee.handleEvent(
					new PersonDepartureEvent(0, new IdImpl( 1 ), ego, "mode") );
			testee.handleEvent(
					new PersonEntersVehicleEvent(os.startEgo, ego, vehId) );
			testee.handleEvent(
					new PersonDepartureEvent(0, new IdImpl( 1 ), alter, "mode") );
			testee.handleEvent(
					new PersonEntersVehicleEvent(os.startAlter, alter, vehId) );
			testee.handleEvent(
					new PersonLeavesVehicleEvent(os.endEgo, ego, vehId) );
			testee.handleEvent(
					new PersonLeavesVehicleEvent(os.endAlter, alter, vehId) );

			Assert.assertEquals(
					"unexpected overlap for "+os,
					0,
					testee.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testOvelapsOfActivitiesInActiveTimeWindow() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";

		final double startWindow = 10;
		final double endWindow = 30;
		
		for ( OverlapSpec os : new OverlapSpec[]{
				new OverlapSpec( 0 , 10 , 20 , 30 ),
				new OverlapSpec( 0 , 10 , 10 , 30 ),
				new OverlapSpec( 0 , 20 , 10 , 30 ),
				new OverlapSpec( 10 , 20 , 10 , 30 ),
				new OverlapSpec( 20 , 30 , 10 , 30 ),
				new OverlapSpec( 30 , 50 , 10 , 30 ) }) {
			final BeingTogetherScoring testee =
				new BeingTogetherScoring(
						null, // facilities
						startWindow,
						endWindow,
						1,
						ego,
						Collections.singleton( alter ) );
			testee.handleEvent(
					new ActivityStartEvent(os.startEgo, ego, linkId, null, type) );
			testee.handleEvent(
					new ActivityStartEvent(os.startAlter, alter, linkId, null, type) );
			testee.handleEvent(
					new ActivityEndEvent(os.endEgo, ego, linkId, null, type) );
			testee.handleEvent(
					new ActivityEndEvent(os.endAlter, alter, linkId, null, type) );

			Assert.assertEquals(
					"unexpected overlap for "+os,
					Math.max(
						Math.min(
							endWindow,
							Math.min(
								os.endAlter,
								os.endEgo ) ) -
						Math.max(
							startWindow,
							Math.max(
								os.startAlter,
								os.startEgo ) ) ,
						0 ),
					testee.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testOvelapsOfActivitiesWithOpeningTimes() {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";

		final ActivityFacilities facilities = new ActivityFacilitiesImpl();
		final ActivityFacility facility =
			facilities.getFactory().createActivityFacility(
					new IdImpl( "facility" ),
					new CoordImpl( 0 , 0 ) );
		facilities.addActivityFacility( facility );
		final ActivityOption option = facilities.getFactory().createActivityOption( type );
		facility.addActivityOption( option );

		final double startFirstWindow = 10;
		final double endFirstWindow = 15;
		option.addOpeningTime( new OpeningTimeImpl( startFirstWindow , endFirstWindow ) );

		final double startSecondWindow = 25;
		final double endSecondWindow = 30;
		option.addOpeningTime( new OpeningTimeImpl( startSecondWindow , endSecondWindow ) );
		
		for ( OverlapSpec os : new OverlapSpec[]{
				new OverlapSpec( 0 , 10 , 20 , 30 ),
				new OverlapSpec( 0 , 10 , 10 , 30 ),
				new OverlapSpec( 0 , 20 , 10 , 30 ),
				new OverlapSpec( 10 , 20 , 10 , 30 ),
				new OverlapSpec( 20 , 30 , 10 , 30 ),
				new OverlapSpec( 30 , 50 , 10 , 30 ) }) {
			final BeingTogetherScoring testee =
				new BeingTogetherScoring(
						facilities, // facilities
						1,
						ego,
						Collections.singleton( alter ) );
			testee.handleEvent(
					new ActivityStartEvent(os.startEgo, ego, linkId, facility.getId(), type) );
			testee.handleEvent(
					new ActivityStartEvent(os.startAlter, alter, linkId, facility.getId(), type) );
			testee.handleEvent(
					new ActivityEndEvent(os.endEgo, ego, linkId, facility.getId(), type) );
			testee.handleEvent(
					new ActivityEndEvent(os.endAlter, alter, linkId, facility.getId(), type) );

			final double startFirstOverlap = 
					Math.max(
							startFirstWindow,
							Math.max(
								os.startEgo,
								os.startAlter ) );
			final double endFirstOverlap = 
					Math.min(
							endFirstWindow,
							Math.min(
								os.endEgo,
								os.endAlter ) );
			final double firstOverlap = Math.max( 0 , endFirstOverlap - startFirstOverlap );

			final double startSecondOverlap = 
					Math.max(
							startSecondWindow,
							Math.max(
								os.startEgo,
								os.startAlter ) );
			final double endSecondOverlap = 
					Math.min(
							endSecondWindow,
							Math.min(
								os.endEgo,
								os.endAlter ) );
			final double secondOverlap = Math.max( 0 , endSecondOverlap - startSecondOverlap );

			Assert.assertEquals(
					"unexpected overlap for "+os,
					firstOverlap + secondOverlap,
					testee.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}


	private static class OverlapSpec {
		public double startEgo, startAlter, endEgo, endAlter;

		public OverlapSpec(
				final double startEgo,
				final double endEgo,
				final double startAlter,
				final double endAlter) {
			this.startEgo = startEgo;
			this.startAlter = startAlter;
			this.endEgo = endEgo;
			this.endAlter = endAlter;
		}

		@Override
		public String toString() {
			return "[OvelapSpec: startEgo="+startEgo+
				", endEgo="+endEgo+
				", startAlter="+startAlter+
				", endAlter="+endAlter+"]";
		}
	}
}

