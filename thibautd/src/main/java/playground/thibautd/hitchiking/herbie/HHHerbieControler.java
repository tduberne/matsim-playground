/* *********************************************************************** *
 * project: org.matsim.*
 * HHHerbieControler.java
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
package playground.thibautd.hitchiking.herbie;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.controler.listeners.CalcLegTimesHerbieListener;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;
import herbie.running.replanning.TransitStrategyManager;
import herbie.running.scoring.HerbieTravelCostCalculatorFactory;
import herbie.running.scoring.TravelScoringFunction;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalties;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.pt.router.TransitRouterConfig;
import playground.thibautd.herbie.HerbiePlanBasedScoringFunctionFactory;
import playground.thibautd.herbie.HerbieTransitRouterFactory;
import playground.thibautd.hitchiking.run.HitchHikingControler;
import playground.thibautd.hitchiking.spotweights.SpotWeighter;

/**
 * @author thibautd
 */
public class HHHerbieControler extends HitchHikingControler {
	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";

	private final HerbieConfigGroup herbieConfigGroup;
	
	public HHHerbieControler(
			final Scenario scenario,
			final SpotWeighter weighter) {
		super( scenario , weighter );
		herbieConfigGroup = (HerbieConfigGroup) super.config.getModule(HerbieConfigGroup.GROUP_NAME);
	}

	@Override
	protected void loadData() {
		super.loadData();
		this.setScenarioLoaded(true);
	}

	@Override
	protected void setUp() {
		FacilityPenalties facPenalties = (FacilityPenalties) getScenario().getScenarioElement(FacilityPenalties.ELEMENT_NAME);

		if (facPenalties == null) {
			// I'm pretty sure it's not used anywhere...
			facPenalties = new FacilityPenalties();
			getScenario().addScenarioElement( FacilityPenalties.ELEMENT_NAME, facPenalties );
		}

        HerbiePlanBasedScoringFunctionFactory herbieScoringFunctionFactory =
			new HerbiePlanBasedScoringFunctionFactory(
				super.config,
				this.herbieConfigGroup,
				facPenalties.getFacilityPenalties(),
                    getScenario().getActivityFacilities(),
                    getScenario().getNetwork());

		this.setScoringFunctionFactory( herbieScoringFunctionFactory );
				
		CharyparNagelScoringParameters params = herbieScoringFunctionFactory.getParams();
		
		HerbieTravelCostCalculatorFactory costCalculatorFactory = new HerbieTravelCostCalculatorFactory(params, this.herbieConfigGroup);
		TravelTime timeCalculator = super.getLinkTravelTimes();
		PlanCalcScoreConfigGroup cnScoringGroup = null;
		costCalculatorFactory.createTravelDisutility(timeCalculator, cnScoringGroup);
		
		this.setTravelDisutilityFactory(costCalculatorFactory);

		// set the TransitRouterFactory rather than a RoutingModuleFactory, so that
		// if some parts of the code use this method, everything should be consistent.
		setTransitRouterFactory(
				new HerbieTransitRouterFactory( 
					getScenario().getTransitSchedule(),
					new TransitRouterConfig(
						config.planCalcScore(),
						config.plansCalcRoute(),
						config.transitRouter(),
						config.vspExperimental()),
					herbieConfigGroup,
					new TravelScoringFunction( params, herbieConfigGroup ) ) );

		super.setUp();
	}
	
	
	private final double reroutingShare = 0.05;
	 /**
	  * Create and return a TransitStrategyManager which filters transit agents
	  * during the replanning phase. They either keep their selected plan or
	  * replan it.
	  */
	 @Override
	 protected StrategyManager loadStrategyManager() {
	  log.info("loading TransitStrategyManager - using rerouting share of " + reroutingShare);
	  StrategyManager manager = new TransitStrategyManager(this, reroutingShare);
	  StrategyManagerConfigLoader.load(this, manager);
	  return manager;
	 }

	@Override
	protected void loadControlerListeners() {
		super.loadControlerListeners();
		//this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesHerbieListener(CALC_LEG_TIMES_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME, this.scenarioData.getNetwork()));
//		this.addControlerListener(new KtiPopulationPreparation(this.ktiConfigGroup));
	}

	//@Override
	//public PlanAlgorithm createRoutingAlgorithm(final TravelDisutility travelCosts, final PersonalizableTravelTime travelTimes) {
	//	PlanAlgorithm router = null;
	//	router = super.createRoutingAlgorithm(travelCosts, travelTimes);
	//	return router;
	//}
}
