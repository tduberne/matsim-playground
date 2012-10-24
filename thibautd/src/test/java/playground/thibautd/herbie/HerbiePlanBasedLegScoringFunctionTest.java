/* *********************************************************************** *
 * project: org.matsim.*
 * HerbiePlanBasedLegScoringFunctionTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.herbie;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.scoring.HerbieScoringFunctionFactory;
import herbie.running.scoring.LegScoringFunction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.population.Desires;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.cliquessim.herbie.scoring.HerbieJointLegScoringFunction;

/**
 * @author thibautd
 */
@RunWith( value = Parameterized.class )
public class HerbiePlanBasedLegScoringFunctionTest {
	private final Plan plan;
	private Network network;
	private Config config;
	private CharyparNagelScoringParameters params;
	private HerbieConfigGroup ktiConfigGroup;
	private ActivityFacilitiesImpl facilities;

	// /////////////////////////////////////////////////////////////////////////
	// construction(s)
	// /////////////////////////////////////////////////////////////////////////
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList( 
				new Object[] {getCarPlan()},
				new Object[] {getPtPlanNoTransfer()},
				new Object[] {getPtPlanTransfer()},
				new Object[] {getWalkPlan()});
	}

	private static Plan getCarPlan() {
		PersonImpl person = new PersonImpl( new IdImpl( "jojo" ) );
		Desires desires = person.createDesires( "bwarf" );
		desires.putActivityDuration( "h" , 12 * 3600 );
		desires.putActivityDuration( "w" , 12 * 3600 );
		PlanImpl plan = new PlanImpl( person );

		Activity act = plan.createAndAddActivity( "h" );
		act.setEndTime( 10 );
		((ActivityImpl) act).setFacilityId( new IdImpl( "h" ) );
		Leg l = plan.createAndAddLeg( TransportMode.car );
		NetworkRoute nRoute = new LinkNetworkRouteImpl( new IdImpl( 12 ) , new IdImpl( 23 ) );
		l.setRoute( nRoute );
		l.setDepartureTime( 123 );
		l.setTravelTime( 456 );

		act = plan.createAndAddActivity( "w" );
		act.setStartTime( 104 );
		act.setEndTime( 294 );
		((ActivityImpl) act).setFacilityId( new IdImpl( "w" ) );
		l = plan.createAndAddLeg( TransportMode.car );
		nRoute = new LinkNetworkRouteImpl( new IdImpl( 43 ) , new IdImpl( 23 ) );
		nRoute.setLinkIds(
				new IdImpl( 43 ) ,
				Arrays.asList( (Id) new IdImpl( 34 ) , new IdImpl( 42 ) , new IdImpl( 23 ) , new IdImpl( 34 ) , new IdImpl( 42 ) ) ,
				new IdImpl( 23 ) );
		l.setRoute( nRoute );
		l.setDepartureTime( 123 );
		l.setTravelTime( 456 );

		act = plan.createAndAddActivity( "h" );
		act.setStartTime( 2000 );
		((ActivityImpl) act).setFacilityId( new IdImpl( "h" ) );

		return plan;
	}

	private static Plan getPtPlanNoTransfer() {
		PersonImpl person = new PersonImpl( new IdImpl( "jojo" ) );
		Desires desires = person.createDesires( "bwarf" );
		desires.putActivityDuration( "h" , 12 * 3600 );
		desires.putActivityDuration( "w" , 12 * 3600 );
		PlanImpl plan = new PlanImpl( person );

		Activity act = plan.createAndAddActivity( "h" );
		act.setEndTime( 10 );
		((ActivityImpl) act).setFacilityId( new IdImpl( "h" ) );
		Leg l = plan.createAndAddLeg( TransportMode.pt );
		ExperimentalTransitRoute tRoute =
			new ExperimentalTransitRoute( new FakeFacility( 23 ) , null , null , new FakeFacility( 43 ) );
		l.setRoute( tRoute );
		l.setDepartureTime( 1123 );
		l.setTravelTime( 3456 );

		plan.createAndAddActivity( "w" );
		act.setStartTime( 100 );
		act.setEndTime( 109 );
		((ActivityImpl) act).setFacilityId( new IdImpl( "w" ) );
		l = plan.createAndAddLeg( TransportMode.pt );
		tRoute =
			new ExperimentalTransitRoute( new FakeFacility( 13 ) , null , null , new FakeFacility( 42 ) );
		l.setRoute( tRoute );
		l.setDepartureTime( 1123 );
		l.setTravelTime( 3456 );
		
		plan.createAndAddActivity( "h" );
		act.setStartTime( 2000 );
		((ActivityImpl) act).setFacilityId( new IdImpl( "h" ) );

		return plan;
	}

	private static Plan getPtPlanTransfer() {
		PersonImpl person = new PersonImpl( new IdImpl( "jojo" ) );
		Desires desires = person.createDesires( "bwarf" );
		desires.putActivityDuration( "h" , 12 * 3600 );
		desires.putActivityDuration( "w" , 12 * 3600 );
		PlanImpl plan = new PlanImpl( person );

		Activity act = plan.createAndAddActivity( "h" );
		act.setEndTime( 100 );
		((ActivityImpl) act).setFacilityId( new IdImpl( "h" ) );
		Leg l = plan.createAndAddLeg( TransportMode.transit_walk );
		GenericRoute route = new GenericRouteImpl( new IdImpl( 12 ) , new IdImpl( 23 ) );
		l.setRoute( route );
		l.setDepartureTime( 123 );
		l.setTravelTime( 456 );

		act = plan.createAndAddActivity( PtConstants.TRANSIT_ACTIVITY_TYPE );
		act.setStartTime( 0 );
		act.setEndTime( 0 );
		l = plan.createAndAddLeg( TransportMode.pt );
		ExperimentalTransitRoute tRoute =
			new ExperimentalTransitRoute( new FakeFacility( 23 ) , null , null , new FakeFacility( 43 ) );
		l.setRoute( tRoute );
		l.setDepartureTime( 1123 );
		l.setTravelTime( 3456 );

		plan.createAndAddActivity( PtConstants.TRANSIT_ACTIVITY_TYPE );
		act.setStartTime( 0 );
		act.setEndTime( 0 );
		l = plan.createAndAddLeg( TransportMode.transit_walk );
		l.setRoute( new GenericRouteImpl( new IdImpl( 23 ) , new IdImpl( 43 ) ) );
		l.setDepartureTime( 1123 );
		l.setTravelTime( 3456 );

		plan.createAndAddActivity( "w" );
		act.setStartTime( 2000 );
		act.setEndTime( 3000 );
		((ActivityImpl) act).setFacilityId( new IdImpl( "w" ) );
		l = plan.createAndAddLeg( TransportMode.transit_walk );
		l.setRoute( new GenericRouteImpl( new IdImpl( 34 ) , new IdImpl( 43 ) ) );
		l.setDepartureTime( 1123 );
		l.setTravelTime( 346 );

		plan.createAndAddActivity( "h" );
		act.setStartTime( 2000 );
		((ActivityImpl) act).setFacilityId( new IdImpl( "h" ) );

		return plan;
	}

	private static Plan getWalkPlan() {
		PersonImpl person = new PersonImpl( new IdImpl( "jojo" ) );
		Desires desires = person.createDesires( "bwarf" );
		desires.putActivityDuration( "h" , 12 * 3600 );
		desires.putActivityDuration( "w" , 12 * 3600 );
		PlanImpl plan = new PlanImpl( person );

		for (double tt=60; tt <= 2*3600; tt *= 1.5) {
			Activity act = plan.createAndAddActivity( "w" );
			act.setStartTime( 2000 );
			act.setEndTime( 3000 );
			((ActivityImpl) act).setFacilityId( new IdImpl( "w" ) );
			Leg l = plan.createAndAddLeg( TransportMode.walk );
			l.setRoute( new GenericRouteImpl( new IdImpl( 12 ) , new IdImpl( 13 ) ) );
			l.setDepartureTime( 1123 );
			l.setTravelTime( tt );
		}
		Activity act = plan.createAndAddActivity( "w" );
		act.setStartTime( 2000 );
		((ActivityImpl) act).setFacilityId( new IdImpl( "w" ) );

		return plan;
	}

	public HerbiePlanBasedLegScoringFunctionTest(final Plan plan) {
		this.plan = plan;
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Before
	public void init() {
		initConfig();
		initNetwork();
		//initPlans();
		initFacilities();
	}

	private void initFacilities() {
		facilities = new ActivityFacilitiesImpl();
		ActivityFacilityImpl fac = facilities.createFacility( new IdImpl( "h" ) , new CoordImpl( 0 , 0 ) );
		fac.createActivityOption( "h" ).addOpeningTime( new OpeningTimeImpl( DayType.wk , 0 , 24 * 3600 ) );
		fac = facilities.createFacility( new IdImpl( "w" ) , new CoordImpl( 0 , 0 ) );
		fac.createActivityOption( "w" ).addOpeningTime( new OpeningTimeImpl( DayType.wk , 7 , 20 * 3600 ) );
	}

	private void initConfig() {
		config = ConfigUtils.createConfig();
		ktiConfigGroup = new HerbieConfigGroup();
		config.addModule( HerbieConfigGroup.GROUP_NAME , ktiConfigGroup );
		config.planCalcScore().setMarginalUtlOfDistanceWalk( -5 );
		config.planCalcScore().setMarginalUtlOfDistanceOther( -9 );
		config.planCalcScore().setMonetaryDistanceCostRateCar( -1 );
		config.planCalcScore().setMonetaryDistanceCostRatePt( -2 );
		params = new CharyparNagelScoringParameters( config.planCalcScore() );
	}

	//private void initPlans() {
	//	testPlans = new ArrayList<Plan>();

	//	testPlans.add( getCarPlan() );
	//	testPlans.add( getPtPlanTransfer() );
	//	testPlans.add( getPtPlanNoTransfer() );
	//	testPlans.add( getWalkPlan() );
	//}


	private void initNetwork() {
		NetworkImpl nImpl = (NetworkImpl) ScenarioUtils.createScenario( config ).getNetwork();
		Node n1 = nImpl.createAndAddNode( new IdImpl( 1 ) , new CoordImpl( 0 , 0 ) );
		Node n2 = nImpl.createAndAddNode( new IdImpl( 2 ) , new CoordImpl( 1 , 0 ) );
		Node n3 = nImpl.createAndAddNode( new IdImpl( 3 ) , new CoordImpl( 1 , 2 ) );
		Node n4 = nImpl.createAndAddNode( new IdImpl( 4 ) , new CoordImpl( 4 , 2 ) );

		nImpl.createAndAddLink( new IdImpl( 12 ) , n1 , n2 , 1 , 1 , 1 , 1 );
		nImpl.createAndAddLink( new IdImpl( 13 ) , n1 , n3 , 1 , 1 , 1 , 1 );
		nImpl.createAndAddLink( new IdImpl( 23 ) , n2 , n3 , 1 , 1 , 1 , 1 );
		nImpl.createAndAddLink( new IdImpl( 43 ) , n4 , n3 , 1 , 1 , 1 , 1 );
		nImpl.createAndAddLink( new IdImpl( 42 ) , n4 , n2 , 1 , 1 , 1 , 1 );
		nImpl.createAndAddLink( new IdImpl( 34 ) , n3 , n4 , 1 , 1 , 1 , 1 );

		network = nImpl;
	}

	@Test
	public void testLegScores() {
		Random random = new Random( 42 );
		double amplitude = 100;

		LegScoring planBased = new PlanBasedLegScoringFunction(
				plan,
				new HerbieJointLegScoringFunction(
					plan, 
					params,
					config,
					this.network,
					this.ktiConfigGroup));
		LegScoring base = new LegScoringFunction(
					plan, 
					params,
					config,
					this.network,
					this.ktiConfigGroup);
		
		int c = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				// use randomized start and end time, so that if one
				// of the scoring function uses the leg ones, it fails.
				double start = leg.getDepartureTime() + (random.nextDouble() * amplitude) - amplitude / 2;
				double end = start + leg.getTravelTime() + (random.nextDouble() * amplitude) - amplitude / 2;
				end = end > start ? end : 0d;
				c++;
				planBased.startLeg( start , leg );
				base.startLeg( start , leg );
				planBased.endLeg( end );
				base.endLeg( end );
			}
		}
		if (c==0) throw new RuntimeException( "nothing tested!" );

		((BasicScoring) planBased).finish();
		((BasicScoring) base).finish();

		Assert.assertEquals(
			((BasicScoring) base).getScore(),
			((BasicScoring) planBased).getScore(),
			MatsimTestUtils.EPSILON);
	}

	@Test
	public void testPlanScores() {
		HerbiePlanBasedScoringFunctionFactory planBasedScoringFunctionFactory =
			new HerbiePlanBasedScoringFunctionFactory(
				config,
				ktiConfigGroup,
				new TreeMap<Id, FacilityPenalty>(),
				facilities,
				network);

		HerbieScoringFunctionFactory baseScoringFunctionFactory =
			new HerbieScoringFunctionFactory(
				config,
				ktiConfigGroup,
				new TreeMap<Id, FacilityPenalty>(),
				facilities,
				network);

		for (int until=1; until < plan.getPlanElements().size(); until++) {
			ScoringFunction planBased = planBasedScoringFunctionFactory.createNewScoringFunction( plan );
			ScoringFunction base = baseScoringFunctionFactory.createNewScoringFunction( plan );
	
			// test all "partial" scores, to track where it fails
			int c = 0;
			for (PlanElement pe : plan.getPlanElements().subList(0,until)) {
				c++;
				if (pe instanceof Leg) {
					planBased.handleLeg( (Leg) pe );
					base.handleLeg( (Leg) pe );
				}
				else {
					planBased.handleActivity( (Activity) pe );
					base.handleActivity( (Activity) pe );
				}
			}
			if (c==0) throw new RuntimeException( "nothing tested!" );

			planBased.finish();
			base.finish();

			Assert.assertEquals(
					"wrong score for "+plan.getPlanElements().subList(0,until),
					base.getScore(),
					planBased.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}

	private static class FakeFacility implements TransitStopFacility {
		private final Id id;
		public FakeFacility(final int id) {
			this.id = new IdImpl( id );
		}

		@Override
		public Id getLinkId() {
			return id;
		}

		@Override
		public Coord getCoord() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Id getId() {
			return id;
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean getIsBlockingLane() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setLinkId(Id linkId) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setName(String name) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Id getStopPostAreaId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setStopPostAreaId(Id stopPostAreaId) {
			// TODO Auto-generated method stub
			
		}
	}
}

