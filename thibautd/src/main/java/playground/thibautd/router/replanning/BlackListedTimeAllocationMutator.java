/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutator.java
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
package playground.thibautd.router.replanning;

import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * A time allocation mutator to use with multi-leg routing.
 *
 * @author thibautd
 */
public class BlackListedTimeAllocationMutator implements PlanAlgorithm {
	private static final Logger log =
		Logger.getLogger(BlackListedTimeAllocationMutator.class);

	private double temperature = 1;
	private final double mutationRange;
	private final StageActivityTypes blackList;
	private final Random random;
	private Setting setting = Setting.MUTATE_END_AS_DUR;

	public enum Setting {
		MUTATE_DUR,
		MUTATE_END,
		MUTATE_END_AS_DUR;
	}

	public BlackListedTimeAllocationMutator(
			final StageActivityTypes blackList,
			final double mutationRange,
			final Random random) {
		this.blackList = blackList;
		this.mutationRange = mutationRange;
		this.random = random;
		log.debug( "setting initialized to "+setting );
	}

	@Override
	public void run(final Plan plan) {
		final List<Activity> activities = TripStructureUtils.getActivities( plan , blackList );
		final int nActs = activities.size();
		// when mutating durations "blindly", avoid creating activities ending before
		// the previous activity.
		double lastEndTime = Double.NEGATIVE_INFINITY;
		for ( Activity a : activities ) {
			switch ( setting ) {
				case MUTATE_DUR:
					((ActivityImpl) a).setMaximumDuration( mutateTime( a.getMaximumDuration() ) );
					break;
				case MUTATE_END:
					a.setEndTime( mutateTime( a.getEndTime() ) );
					if ( a.getEndTime() < lastEndTime ) {
						a.setEndTime( lastEndTime );
					}
					lastEndTime = a.getEndTime();
					break;
				case MUTATE_END_AS_DUR:
					final double oldTime = a.getEndTime();
					final double newTime = mutateTime( oldTime );
					// doing this so rather than sampling mut directly allows
					// to avoid negative times
					final double mut = newTime - oldTime;
					// shift all times after the mutated time (as if we were working on durations)
					for ( Activity currAct : activities.subList( activities.indexOf( a ) , nActs ) ) {
						currAct.setEndTime( currAct.getEndTime() + mut );
					}
					break;
				default:
					throw new RuntimeException( "what is that? "+setting );
			}
		}
	}

	private double mutateTime(final double time) {
		// do not do anything if time is undefined
		if ( time == Time.UNDEFINED_TIME ) return time;

		final double actualRange = temperature * mutationRange;
		final double t = time + (int)((this.random.nextDouble() * 2.0 - 1.0) * actualRange);
		return t < 0 ? 0 : t;
	}

	/**
	 * @param t a constant by which to multiply the mutation range.
	 * If used, this should start high and decrease with iterations.
	 */
	public void setTemperature(final double t) {
		if ( t < 0 ) throw new IllegalArgumentException();
		if ( t < 1 ) log.warn( "temperature below 1 is discouraged, as the meaning of the mutation range becomes dubious" );
		this.temperature = t;
	}

	public void setSetting(final Setting setting) {
		log.debug( "setting set to "+setting );
		this.setting = setting;
	}
}

