/* *********************************************************************** *
 * project: org.matsim.*
 * ElementalCharyparNagelLegScoringFunction.java
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

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.core.utils.misc.RouteUtils;

/**
 * @author thibautd
 */
public class ElementalCharyparNagelLegScoringFunction implements LegScoring {
	private static final Logger log =
		Logger.getLogger(ElementalCharyparNagelLegScoringFunction.class);

	private double score = 0;

	private final String mode;
	private final LegScoringParameters params;
	private final Network network;

	public ElementalCharyparNagelLegScoringFunction(
			final String mode,
			final LegScoringParameters params,
			final Network network) {
		this.mode = mode;
		this.params = params;
		this.network = network;
		this.reset();
	}

	@Override
	public void reset() {
		this.score = 0;
	}

	public void handleLeg( final Leg leg ) {
		score += calcLegScore( leg );
		if ( log.isTraceEnabled() ) {
			log.trace( "new score for mode "+mode+" for leg "+leg+": "+score );
		}
	}

	@Override
	public void startLeg(final double time, final Leg leg) {
		handleLeg( leg );
	}

	@Override
	public void endLeg(final double time) {}

	@Override
	public void finish() {}

	@Override
	public double getScore() {
		return this.score;
	}

	private double calcLegScore(final Leg leg) {
		if ( !leg.getMode().equals( mode ) ) return 0;

		final double dist =
			// I took this because it was in the core function,
			// but I am not sure it does what it should,
			// as there are several values
			// of zero in the IEEE standard...
			params.marginalUtilityOfDistance_m != 0.0 ?
				getDistance( leg.getRoute() ) :
				0;

		return params.constant +
			params.marginalUtilityOfTraveling_s * leg.getTravelTime() +
			params.marginalUtilityOfDistance_m * dist;
	}

	protected double getDistance(Route route) {
		if (route instanceof NetworkRoute) {
			return  RouteUtils.calcDistance((NetworkRoute) route, network);
		}

		return route.getDistance();
	}

	public static class LegScoringParameters {
		private final double constant;
		private final double marginalUtilityOfTraveling_s;
		private final double marginalUtilityOfDistance_m;

		public LegScoringParameters(
				final double constant,
				final double marginalUtilityOfTraveling_s,
				final double marginalUtilityOfDistance_m) {
			this.constant = constant;
			this.marginalUtilityOfTraveling_s = marginalUtilityOfTraveling_s;
			this.marginalUtilityOfDistance_m = marginalUtilityOfDistance_m;
		}

		public static LegScoringParameters createForCar(final CharyparNagelScoringParameters params) {
			return new LegScoringParameters(
					params.constantCar,
					params.marginalUtilityOfTraveling_s,
					params.marginalUtilityOfDistanceCar_m);
		}

		public static LegScoringParameters createForPt(final CharyparNagelScoringParameters params) {
			return new LegScoringParameters(
					params.constantPt,
					params.marginalUtilityOfTravelingPT_s,
					params.marginalUtilityOfDistancePt_m);
		}

		public static LegScoringParameters createForBike(final CharyparNagelScoringParameters params) {
			return new LegScoringParameters(
					params.constantBike,
					params.marginalUtilityOfTravelingBike_s,
					// Bike has no such setting.
					0 );
		}

		public static LegScoringParameters createForWalk(final CharyparNagelScoringParameters params) {
			return new LegScoringParameters(
					params.constantWalk,
					params.marginalUtilityOfTravelingWalk_s,
					params.marginalUtilityOfDistanceWalk_m);
		}
	}
}