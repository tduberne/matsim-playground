/* *********************************************************************** *
 * project: org.matsim.*
 * FixedRouteLegTravelTimeEstimatorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.thibautd.agentsmating.logitbasedmating.utils;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.old.PlanRouterAdapter;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.trafficmonitoring.DepartureDelayAverageCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;


public class FixedRouteLegTravelTimeEstimatorTest extends MatsimTestCase {

	protected Scenario scenario = null;

	protected static final Id TEST_PERSON_ID = new IdImpl("1");
	private static final int TEST_PLAN_NR = 0;
	private static final int TEST_LEG_NR = 0;
	private static final int TIME_BIN_SIZE = 900;

	protected Person testPerson = null;
	protected Plan testPlan = null;
	protected LegImpl testLeg = null;
	protected ActivityImpl originAct = null;
	protected ActivityImpl destinationAct = null;
	protected Config config = null;

	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";

	private static final Logger log = Logger.getLogger(FixedRouteLegTravelTimeEstimatorTest.class);

	@Override
	protected void setUp() throws Exception {

		super.setUp();

		this.config = super.loadConfig(CONFIGFILE);
		this.config.plans().setInputFile("test/scenarios/equil/plans1.xml");

		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(this.config);
		loader.loadScenario();
		this.scenario = loader.getScenario();

		// the estimator is tested on the central route alternative through equil-net
		// first person
		this.testPerson = this.scenario.getPopulation().getPersons().get(TEST_PERSON_ID);
		// only plan of that person
		this.testPlan = this.testPerson.getPlans().get(TEST_PLAN_NR);
		// first leg
		List<? extends PlanElement> actsLegs = this.testPlan.getPlanElements();
		this.testLeg = (LegImpl) actsLegs.get(TEST_LEG_NR + 1);
		// activities before and after leg
		this.originAct = (ActivityImpl) actsLegs.get(TEST_LEG_NR);
		this.destinationAct = (ActivityImpl) actsLegs.get(TEST_LEG_NR + 2);

		this.config.travelTimeCalculator().setTraveltimeBinSize(TIME_BIN_SIZE);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.scenario = null;
		this.destinationAct = null;
		this.originAct = null;
		this.testLeg = null;
		this.testPerson = null;
		this.testPlan = null;
	}

	public void testGetLegTravelTimeEstimation() {

//		this.scenario.getConfig().charyparNagelScoring().setMarginalUtlOfDistanceCar(0.0);
		this.scenario.getConfig().planCalcScore().setMonetaryDistanceCostRateCar(0.0) ;
		this.scenario.getConfig().planCalcScore().setMarginalUtilityOfMoney(1.) ; 


		for (PlanomatConfigGroup.SimLegInterpretation simLegInterpretation : PlanomatConfigGroup.SimLegInterpretation.values()) {

			DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(this.scenario.getNetwork(), TIME_BIN_SIZE);
			TravelTimeCalculator linkTravelTimeEstimator = new TravelTimeCalculator(this.scenario.getNetwork(), this.config.travelTimeCalculator());

			TravelDisutilityFactory disutilityFactory =
				new TravelDisutilityFactory() {
						@Override
						public TravelDisutility createTravelDisutility(
								TravelTime timeCalculator,
								PlanCalcScoreConfigGroup cnScoringGroup) {
							return new TravelTimeAndDistanceBasedTravelDisutility(
								timeCalculator,
								cnScoringGroup);
						}
					};
			TripRouterFactory tripRouterFactory = new TripRouterFactoryImpl(
					scenario,
					disutilityFactory,
					linkTravelTimeEstimator,
					new DijkstraFactory(),
					null);

			PlanRouterAdapter plansCalcRoute = new PlanRouterAdapter(
					new PlanRouter( tripRouterFactory.createTripRouter() , null ),
					scenario.getNetwork(),
					scenario.getPopulation().getFactory(),
					linkTravelTimeEstimator,
					disutilityFactory.createTravelDisutility( linkTravelTimeEstimator , scenario.getConfig().planCalcScore() ),
					new DijkstraFactory());

			LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(linkTravelTimeEstimator, tDepDelayCalc);
			FixedRouteLegTravelTimeEstimator testee = (FixedRouteLegTravelTimeEstimator) legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
					this.testPlan,
					simLegInterpretation,
					PlanomatConfigGroup.RoutingCapability.fixedRoute,
					plansCalcRoute,
					this.scenario.getNetwork());

			EventsManagerImpl events = (EventsManagerImpl) EventsUtils.createEventsManager();
			events.addHandler(tDepDelayCalc);
			events.addHandler(linkTravelTimeEstimator);
			events.printEventHandlers();

			NetworkRoute route = (NetworkRoute) this.testLeg.getRoute();
			List<Id> linkIds = route.getLinkIds();

			// let's test a route without events first
			// should result in free speed travel time, without departure delay
			double departureTime = Time.parseTime("06:03:00");

			LegImpl newLeg = null;
			newLeg = testee.getNewLeg(this.testLeg.getMode(), this.originAct, this.destinationAct, ((PlanImpl) this.testPlan).getActLegIndex(this.testLeg), departureTime);

			double legTravelTime = 0.0;
			legTravelTime = newLeg.getTravelTime();

			double expectedLegEndTime = departureTime;

			if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				expectedLegEndTime += ((LinkImpl) this.scenario.getNetwork().getLinks().get(this.originAct.getLinkId())).getFreespeedTravelTime();
			}
			for (Id linkId : linkIds) {
				Link link = this.scenario.getNetwork().getLinks().get(linkId);
				expectedLegEndTime += link.getLength()/link.getFreespeed();
			}
			if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				expectedLegEndTime += ((LinkImpl) this.scenario.getNetwork().getLinks().get(this.destinationAct.getLinkId())).getFreespeedTravelTime();
			}
			assertEquals(expectedLegEndTime, departureTime + legTravelTime, MatsimTestCase.EPSILON);

			// next, a departure delay of 5s at the origin link is added
			departureTime = Time.parseTime("06:05:00");
			double depDelay = Time.parseTime("00:00:05");
			AgentDepartureEvent depEvent = new AgentDepartureEvent(
					departureTime,
					TEST_PERSON_ID,
					this.originAct.getLinkId(),
					TransportMode.car);
			LinkLeaveEvent leaveEvent = new LinkLeaveEvent(departureTime + depDelay, this.testPerson.getId(), this.originAct.getLinkId(), null);

			for (Event event : new Event[]{depEvent, leaveEvent}) {
				events.processEvent(event);
			}

			newLeg = testee.getNewLeg(this.testLeg.getMode(), this.originAct, this.destinationAct, ((PlanImpl) this.testPlan).getActLegIndex(this.testLeg), departureTime);
			legTravelTime = newLeg.getTravelTime();

			expectedLegEndTime = departureTime;
			expectedLegEndTime += depDelay;

			if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				expectedLegEndTime += ((LinkImpl) this.scenario.getNetwork().getLinks().get(this.originAct.getLinkId())).getFreespeedTravelTime(Time.UNDEFINED_TIME);
			}
			for (Id linkId : linkIds) {
				Link link = this.scenario.getNetwork().getLinks().get(linkId);
				expectedLegEndTime += link.getLength()/link.getFreespeed();
			}
			if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				expectedLegEndTime += ((LinkImpl) this.scenario.getNetwork().getLinks().get(this.destinationAct.getLinkId())).getFreespeedTravelTime(Time.UNDEFINED_TIME);
			}
			assertEquals(expectedLegEndTime, departureTime + legTravelTime, MatsimTestCase.EPSILON);

			// now let's add some travel events
			// test a start time where all link departures will be in the first time bin
			String[][] eventTimes = new String[][]{
					new String[]{"06:05:00", "06:07:00", "06:09:00"},
					new String[]{"06:16:00", "06:21:00", "06:26:00"}
			};

			Event event = null;
			for (int eventTimesCnt = 0; eventTimesCnt < eventTimes.length; eventTimesCnt++) {
				for (int linkCnt = 0; linkCnt < linkIds.size(); linkCnt++) {
					event = new LinkEnterEvent(
							Time.parseTime(eventTimes[eventTimesCnt][linkCnt]),
							this.testPerson.getId(),
							linkIds.get(linkCnt), null);
					events.processEvent(event);
					event = new LinkLeaveEvent(
							Time.parseTime(eventTimes[eventTimesCnt][linkCnt + 1]),
							this.testPerson.getId(),
							linkIds.get(linkCnt), null);
					events.processEvent(event);
				}
			}

			departureTime = Time.parseTime("06:10:00");

			newLeg = testee.getNewLeg(this.testLeg.getMode(), this.originAct, this.destinationAct, ((PlanImpl) this.testPlan).getActLegIndex(this.testLeg), departureTime);
			legTravelTime = newLeg.getTravelTime();

			expectedLegEndTime = departureTime;
			expectedLegEndTime += depDelay;

			if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				expectedLegEndTime = testee.processLink(this.scenario.getNetwork().getLinks().get(this.originAct.getLinkId()), expectedLegEndTime);
			}
			expectedLegEndTime = testee.processRouteTravelTime(NetworkUtils.getLinks(this.scenario.getNetwork(), route.getLinkIds()), expectedLegEndTime);
			if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				expectedLegEndTime = testee.processLink(this.scenario.getNetwork().getLinks().get(this.destinationAct.getLinkId()), expectedLegEndTime);
			}

			assertEquals(expectedLegEndTime, departureTime + legTravelTime, EPSILON);

			// test public transport mode
			departureTime = Time.parseTime("06:10:00");

			newLeg = testee.getNewLeg(TransportMode.pt, this.originAct, this.destinationAct, ((PlanImpl) this.testPlan).getActLegIndex(this.testLeg), departureTime);
			legTravelTime = newLeg.getTravelTime();

			// the free speed travel time from h to w in equil-test, as simulated by Cetin, is 15 minutes
			expectedLegEndTime = departureTime + (2 * Time.parseTime("00:15:00"));

			// quite a high epsilon here, due to rounding of the free speed in the network.xml file
			// which is 27.78 m/s, but should be 27.777777... m/s, reflecting 100 km/h
			// and 5.0 seconds travel time estimation error is not _that_ bad
			double freeSpeedEpsilon = 5.0;
			assertEquals(expectedLegEndTime, departureTime + legTravelTime, freeSpeedEpsilon);

		}
	}

	public void testProcessDeparture() {

		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(this.scenario.getNetwork(), TIME_BIN_SIZE);
		TravelTimeCalculator linkTravelTimeEstimator = new TravelTimeCalculator(this.scenario.getNetwork(), this.config.travelTimeCalculator());

		TravelDisutilityFactory disutilityFactory =
			new TravelDisutilityFactory() {
					@Override
					public TravelDisutility createTravelDisutility(
							TravelTime timeCalculator,
							PlanCalcScoreConfigGroup cnScoringGroup) {
						return new TravelTimeAndDistanceBasedTravelDisutility(
							timeCalculator,
							cnScoringGroup);
					}
				};
		TripRouterFactory tripRouterFactory = new TripRouterFactoryImpl(
				scenario,
				disutilityFactory,
				linkTravelTimeEstimator,
				new DijkstraFactory(),
				null);

		PlanRouterAdapter plansCalcRoute = new PlanRouterAdapter(
				new PlanRouter( tripRouterFactory.createTripRouter() , null ),
				scenario.getNetwork(),
				scenario.getPopulation().getFactory(),
				linkTravelTimeEstimator,
				disutilityFactory.createTravelDisutility( linkTravelTimeEstimator , scenario.getConfig().planCalcScore() ),
				new DijkstraFactory());

		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(linkTravelTimeEstimator, tDepDelayCalc);

		FixedRouteLegTravelTimeEstimator testee = (FixedRouteLegTravelTimeEstimator) legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				null,
				PlanomatConfigGroup.SimLegInterpretation.CetinCompatible,
				PlanomatConfigGroup.RoutingCapability.fixedRoute,
				plansCalcRoute, this.scenario.getNetwork());
		Id linkId = this.originAct.getLinkId();

		EventsManagerImpl events = (EventsManagerImpl) EventsUtils.createEventsManager();
		events.addHandler(tDepDelayCalc);
		events.printEventHandlers();

		// this gives a delay of 36s (1/100th of an hour)
		AgentDepartureEvent depEvent = new AgentDepartureEvent(6.03 * 3600, TEST_PERSON_ID, this.originAct.getLinkId(), TransportMode.car);
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(6.04 * 3600, TEST_PERSON_ID, this.originAct.getLinkId(), null);

		for (Event event : new Event[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}

		double startTime = 6.00 * 3600;
		double delayEndTime = testee.processDeparture(new IdImpl("1"), startTime);
		assertEquals(delayEndTime, startTime + 36.0, EPSILON);

		// let's add another delay of 72s, should result in an average of 54s
		depEvent = new AgentDepartureEvent(6.02 * 3600, TEST_PERSON_ID, linkId, TransportMode.car);
		leaveEvent = new LinkLeaveEvent(6.04 * 3600, TEST_PERSON_ID, linkId, null);

		for (Event event : new Event[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}

		startTime = 6.00 * 3600;
		delayEndTime = testee.processDeparture(linkId, startTime);
		assertEquals(delayEndTime, startTime + (36.0 + 72.0) / 2, EPSILON);

		// the time interval for the previously tested events was for departure times from 6.00 to 6.25
		// for other time intervals, we don't have event information, so estimated delay should be 0s

		startTime = 5.9 * 3600;
		delayEndTime = testee.processDeparture(linkId, startTime);
		assertEquals(delayEndTime, startTime, EPSILON);

		startTime = 6.26 * 3600;
		delayEndTime = testee.processDeparture(linkId, 6.26 * 3600);
		assertEquals(delayEndTime, startTime, EPSILON);

	}

	public void testProcessRouteTravelTime() {

		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(this.scenario.getNetwork(), TIME_BIN_SIZE);
		TravelTimeCalculator linkTravelTimeEstimator = new TravelTimeCalculator(this.scenario.getNetwork(), this.config.travelTimeCalculator());

		TravelDisutilityFactory disutilityFactory =
			new TravelDisutilityFactory() {
					@Override
					public TravelDisutility createTravelDisutility(
							TravelTime timeCalculator,
							PlanCalcScoreConfigGroup cnScoringGroup) {
						return new TravelTimeAndDistanceBasedTravelDisutility(
							timeCalculator,
							cnScoringGroup);
					}
				};
		TripRouterFactory tripRouterFactory = new TripRouterFactoryImpl(
				scenario,
				disutilityFactory,
				linkTravelTimeEstimator,
				new DijkstraFactory(),
				null);

		PlanRouterAdapter plansCalcRoute = new PlanRouterAdapter(
				new PlanRouter( tripRouterFactory.createTripRouter() , null ),
				scenario.getNetwork(),
				scenario.getPopulation().getFactory(),
				linkTravelTimeEstimator,
				disutilityFactory.createTravelDisutility( linkTravelTimeEstimator , scenario.getConfig().planCalcScore() ),
				new DijkstraFactory());

		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(linkTravelTimeEstimator, tDepDelayCalc);

		FixedRouteLegTravelTimeEstimator testee = (FixedRouteLegTravelTimeEstimator) legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				null,
				PlanomatConfigGroup.SimLegInterpretation.CetinCompatible,
				PlanomatConfigGroup.RoutingCapability.fixedRoute,
				plansCalcRoute,
				this.scenario.getNetwork());

		EventsManagerImpl events = (EventsManagerImpl) EventsUtils.createEventsManager();
		events.addHandler(linkTravelTimeEstimator);
		events.printEventHandlers();

		NetworkRoute route = (NetworkRoute) this.testLeg.getRoute();
		log.info(route.toString());

		// generate some travel times
		Event event = null;

		List<Id> linkIds = route.getLinkIds();
		System.out.println(linkIds.size());

		String[][] eventTimes = new String[][]{
			new String[]{"06:05:00", "06:07:00", "06:09:00"},
			new String[]{"06:16:00", "06:21:00", "06:26:00"}
		};

		for (int eventTimesCnt = 0; eventTimesCnt < eventTimes.length; eventTimesCnt++) {
			for (int linkCnt = 0; linkCnt < linkIds.size(); linkCnt++) {
				event = new LinkEnterEvent(
						Time.parseTime(eventTimes[eventTimesCnt][linkCnt]),
						this.testPerson.getId(),
						linkIds.get(linkCnt), null);
				events.processEvent(event);
				event = new LinkLeaveEvent(
						Time.parseTime(eventTimes[eventTimesCnt][linkCnt + 1]),
						this.testPerson.getId(),
						linkIds.get(linkCnt), null);
				events.processEvent(event);
			}
		}

		// test a start time where all link departures will be in the first time bin
		double startTime = Time.parseTime("06:10:00");
		double routeEndTime = testee.processRouteTravelTime(NetworkUtils.getLinks(this.scenario.getNetwork(), route.getLinkIds()), startTime);
		assertEquals(Time.parseTime("06:14:00"), routeEndTime, EPSILON);

		// test a start time where all link departures will be in the second time bin
		startTime = Time.parseTime("06:20:00");
		routeEndTime = testee.processRouteTravelTime(NetworkUtils.getLinks(this.scenario.getNetwork(), route.getLinkIds()), startTime);
		assertEquals(Time.parseTime("06:30:00"), routeEndTime, EPSILON);

		// test a start time in the first bin where one link departure is in the first bin, one in the second bin
		startTime = Time.parseTime("06:13:00");
		routeEndTime = testee.processRouteTravelTime(NetworkUtils.getLinks(this.scenario.getNetwork(), route.getLinkIds()), startTime);
		assertEquals(Time.parseTime("06:20:00"), routeEndTime, EPSILON);

		// test a start time in a free speed bin, having second departure in the first bin
		startTime = Time.parseTime("05:59:00");
		routeEndTime = testee.processRouteTravelTime(NetworkUtils.getLinks(this.scenario.getNetwork(), route.getLinkIds()), startTime);
		assertEquals(
				testee.processLink(this.scenario.getNetwork().getLinks().get(linkIds.get(1)), startTime + ((LinkImpl) this.scenario.getNetwork().getLinks().get(linkIds.get(0))).getFreespeedTravelTime()),
				routeEndTime, EPSILON);

		// test a start time in the second bin, having second departure in the free speed bin
		startTime = Time.parseTime("06:28:00");
		routeEndTime = testee.processRouteTravelTime(NetworkUtils.getLinks(this.scenario.getNetwork(), route.getLinkIds()), startTime);
		assertEquals(
				testee.processLink(this.scenario.getNetwork().getLinks().get(linkIds.get(0)), startTime) + ((LinkImpl) this.scenario.getNetwork().getLinks().get(linkIds.get(1))).getFreespeedTravelTime(),
				routeEndTime, EPSILON);

	}

	public void testProcessLink() {

		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(this.scenario.getNetwork(), TIME_BIN_SIZE);
		TravelTimeCalculator linkTravelTimeEstimator = new TravelTimeCalculator(this.scenario.getNetwork(), this.config.travelTimeCalculator());

		TravelDisutilityFactory disutilityFactory =
			new TravelDisutilityFactory() {
					@Override
					public TravelDisutility createTravelDisutility(
							TravelTime timeCalculator,
							PlanCalcScoreConfigGroup cnScoringGroup) {
						return new TravelTimeAndDistanceBasedTravelDisutility(
							timeCalculator,
							cnScoringGroup);
					}
				};
		TripRouterFactory tripRouterFactory = new TripRouterFactoryImpl(
				scenario,
				disutilityFactory,
				linkTravelTimeEstimator,
				new DijkstraFactory(),
				null);

		PlanRouterAdapter plansCalcRoute = new PlanRouterAdapter(
				new PlanRouter( tripRouterFactory.createTripRouter() , null ),
				scenario.getNetwork(),
				scenario.getPopulation().getFactory(),
				linkTravelTimeEstimator,
				disutilityFactory.createTravelDisutility( linkTravelTimeEstimator , scenario.getConfig().planCalcScore() ),
				new DijkstraFactory());

		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(linkTravelTimeEstimator, tDepDelayCalc);

		FixedRouteLegTravelTimeEstimator testee = (FixedRouteLegTravelTimeEstimator) legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				null,
				PlanomatConfigGroup.SimLegInterpretation.CetinCompatible,
				PlanomatConfigGroup.RoutingCapability.fixedRoute,
				plansCalcRoute,
				this.scenario.getNetwork());

		Id linkId = ((NetworkRoute) this.testLeg.getRoute()).getLinkIds().get(0);

		EventsManagerImpl events = (EventsManagerImpl) EventsUtils.createEventsManager();
		events.addHandler(linkTravelTimeEstimator);
		events.printEventHandlers();

		// we have one agent on this link, taking 1 minute and 48 seconds
		LinkEnterEvent enterEvent = new LinkEnterEvent(Time.parseTime("06:05:00"), this.testPerson.getId(), linkId, null);
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(Time.parseTime("06:06:48"), this.testPerson.getId(), linkId, null);

		for (Event event : new Event[]{enterEvent, leaveEvent}) {
			events.processEvent(event);
		}

		// for start times inside the time bin, the predicted travel time is always the same
		double startTime = Time.parseTime("06:10:00");
		double linkEndTime = testee.processLink(this.scenario.getNetwork().getLinks().get(linkId), startTime);
		assertEquals(linkEndTime, Time.parseTime("06:11:48"), EPSILON);

		startTime = Time.parseTime("06:01:00");
		linkEndTime = testee.processLink(this.scenario.getNetwork().getLinks().get(linkId), startTime);
		assertEquals(linkEndTime, Time.parseTime("06:02:48"), EPSILON);

		// for start times outside the time bin, the free speed travel time is returned
		double freeSpeedTravelTime = ((LinkImpl) ((NetworkImpl)this.scenario.getNetwork()).getLinks().get(linkId)).getFreespeedTravelTime(Time.UNDEFINED_TIME);

		startTime = Time.parseTime("05:59:00");
		linkEndTime = testee.processLink(this.scenario.getNetwork().getLinks().get(linkId), startTime);
		assertEquals(startTime + freeSpeedTravelTime, linkEndTime, EPSILON);

		startTime = Time.parseTime("08:12:00");
		linkEndTime = testee.processLink(this.scenario.getNetwork().getLinks().get(linkId), startTime);
		assertEquals(startTime + freeSpeedTravelTime, linkEndTime, EPSILON);

	}

}
