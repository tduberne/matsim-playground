/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripInsertorAndRemoverAlgorithm.java
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
package playground.thibautd.socnetsim.cliques.replanning.modules.jointtripinsertor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripRouter;

import playground.thibautd.socnetsim.cliques.config.JointTripInsertorConfigGroup;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.utils.RoutingUtils;

/**
 * @author thibautd
 */
public class JointTripInsertorAndRemoverAlgorithm implements GenericPlanAlgorithm<JointPlan> {
	private static final Logger log =
		Logger.getLogger(JointTripInsertorAndRemoverAlgorithm.class);

	private final TripRouter tripRouter;
	private final Random random;
	private final JointTripInsertorAlgorithm insertor;
	private final JointTripRemoverAlgorithm remover;
	private final boolean iterative;


	public JointTripInsertorAndRemoverAlgorithm(
			final Config config,
			final TripRouter tripRouter,
			final Random random) {
		this( config , tripRouter , random , false );
	}
	public JointTripInsertorAndRemoverAlgorithm(
			final Config config,
			final TripRouter tripRouter,
			final Random random,
			final boolean iterative) {
		this.tripRouter = tripRouter;
		this.random = random;
		this.insertor = new JointTripInsertorAlgorithm(
				random,
				(JointTripInsertorConfigGroup) config.getModule( JointTripInsertorConfigGroup.GROUP_NAME ),
				tripRouter);
		this.remover = new JointTripRemoverAlgorithm( random );
		this.iterative = iterative;
	}

	@Override
	public void run(final JointPlan plan) {
		if (log.isTraceEnabled()) log.trace( "handling plan "+plan );
		final List<Id> agentsToIgnore = new ArrayList<Id>();
		ActedUponInformation actedUpon = null;

		do {
			if ( random.nextDouble() < getProbRemoval( plan , agentsToIgnore )) {
				actedUpon = remover.run( plan , agentsToIgnore );
				if (log.isTraceEnabled()) log.trace( "removal: "+actedUpon );
			}
			else {
				actedUpon = insertor.run( plan , agentsToIgnore );
				if (log.isTraceEnabled()) log.trace( "insertion: "+actedUpon );
			}

			if (actedUpon == null) return;
			agentsToIgnore.add( actedUpon.getDriverId() );
			agentsToIgnore.add( actedUpon.getPassengerId() );
		} while ( iterative );
	}

	private double getProbRemoval(
			final JointPlan plan,
			final Collection<Id> agentsToIgnore) {
		int countPassengers = 0;
		int countEgoists = 0;
		for (Plan indivPlan : plan.getIndividualPlans().values()) {
			if ( agentsToIgnore.contains( indivPlan.getPerson().getId() ) ) continue;
			List<PlanElement> struct =
					RoutingUtils.tripsToLegs(
							indivPlan,
							tripRouter.getStageActivityTypes(),
							new MainModeIdentifierImpl());
			// parse trips, and count "egoists" (non-driver non-passenger) and
			// passengers. Some care is needed: joint trips are not identified as
			// trips by the router!
			boolean first = true;
			boolean isPassenger = false;
			boolean isDriver = false;
			for (PlanElement pe : struct) {
				if (first) {
					first = false;
				}
				else if (pe instanceof Activity) {
					if (JointActingTypes.JOINT_STAGE_ACTS.isStageActivity( ((Activity) pe).getType() )) {
						// skip
					}
					else if (isPassenger) {
						countPassengers++;
						isPassenger = false;
					}
					else if (isDriver) {
						isDriver = false;
					}
					else {
						countEgoists++;
					}
				}
				else if ( ((Leg) pe).getMode().equals( JointActingTypes.PASSENGER ) ) {
					isPassenger = true;
				}
				else if ( ((Leg) pe).getMode().equals( JointActingTypes.DRIVER ) ) {
					isDriver = true;
				}
			}
		}

		return ((double) countPassengers) / (countEgoists + countPassengers);
	}

}

