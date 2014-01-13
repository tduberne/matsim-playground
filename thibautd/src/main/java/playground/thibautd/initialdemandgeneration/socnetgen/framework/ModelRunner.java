/* *********************************************************************** *
 * project: org.matsim.*
 * ModelRunner.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;

/**
 * @author thibautd
 */
public class ModelRunner<T extends Agent> {
	private static final Logger log =
		Logger.getLogger(ModelRunner.class);

	private int randomSeed = 20130226;
	private int stepSizePrimary = 1;
	private int stepSizeSecondary = 1;
	private UtilityFunction<T> utilityFunction = null;
	private ThresholdFunction thresholds = null;

	// /////////////////////////////////////////////////////////////////////////
	// configuration ( setters )
	// /////////////////////////////////////////////////////////////////////////
	public void setRandomSeed(final int randomSeed) {
		this.randomSeed = randomSeed;
	}

	public void setStepSizePrimary(int stepSize) {
		if ( stepSize <= 0 ) throw new IllegalArgumentException( stepSize+" is negative" );
		this.stepSizePrimary = stepSize;
	}

	public void setStepSizeSecondary(int stepSize) {
		if ( stepSize <= 0 ) throw new IllegalArgumentException( stepSize+" is negative" );
		this.stepSizeSecondary = stepSize;
	}

	public void setUtilityFunction(final UtilityFunction<T> utilityFunction) {
		this.utilityFunction = utilityFunction;
	}

	public void setThresholds(final ThresholdFunction thresholds) {
		this.thresholds = thresholds;
	}

	public ThresholdFunction getThresholds() {
		return thresholds;
	}

	// /////////////////////////////////////////////////////////////////////////
	// run
	// /////////////////////////////////////////////////////////////////////////
	public SocialNetwork run(final SocialPopulation<T> population) {
		if ( utilityFunction == null || thresholds == null ) {
			throw new IllegalStateException( "utility="+utilityFunction+"; thresholds="+thresholds );
		}
		log.info( "create primary ties using step size "+stepSizePrimary );
		final SocialNetwork primaryNetwork = runPrimary( population );

		log.info( "create secondary ties using step size "+stepSizeSecondary );
		try {
			return runSecondary( primaryNetwork , population , Long.MAX_VALUE );
		}
		catch (SecondaryTieLimitExceededException e) {
			throw new RuntimeException( "limit exceeded while not specified!?" , e );
		}
	}

	public SocialNetwork runPrimary(final SocialPopulation<T> population) {
		if ( utilityFunction == null || thresholds == null ) {
			throw new IllegalStateException( "utility="+utilityFunction+"; thresholds="+thresholds );
		}
		final Random random = new Random( randomSeed );
		final SocialNetwork network = new SocialNetwork( population );

		log.info( "create primary ties using step size "+stepSizePrimary );
		fillInPrimaryTies( random , network , population );

		return network;
	}

	public SocialNetwork runSecondary(
			final SocialNetwork primaryNetwork,
			final SocialPopulation<T> population,
			final long maxNSecondaryTies) throws SecondaryTieLimitExceededException {
		if ( utilityFunction == null || thresholds == null ) {
			throw new IllegalStateException( "utility="+utilityFunction+"; thresholds="+thresholds );
		}

		final Random random = new Random( randomSeed + 20140107 );
		final SocialNetwork network = new SocialNetwork( primaryNetwork );

		log.info( "create secondary ties using step size "+stepSizeSecondary );
		fillInSecondaryTies( random , primaryNetwork , network , population , maxNSecondaryTies );

		return network;
	}

	public SocialNetwork runSecondary(
			final SocialNetwork primaryNetwork,
			final SocialPopulation<T> population) {
		try {
			return runSecondary( primaryNetwork , population , Long.MAX_VALUE );
		}
		catch (SecondaryTieLimitExceededException e) {
			throw new RuntimeException( "limit exceeded while not set !?" , e );
		}
	}

	private void fillInPrimaryTies(
			final Random random,
			final SocialNetwork network,
			final SocialPopulation<T> population) {
		final List<T> remainingAgents = new ArrayList<T>( population.getAgents() );

		final Counter counter = new Counter( "consider primary pair # " );
		while ( !remainingAgents.isEmpty() ) {
			final T ego = remainingAgents.remove( random.nextInt( remainingAgents.size() ) );

			final List<T> potentialAlters = new ArrayList<T>( remainingAgents );
			int nAltersToConsider = (int) (remainingAgents.size() / ((double) stepSizePrimary));

			while ( nAltersToConsider-- > 0 && !potentialAlters.isEmpty() ) {
				counter.incCounter();
				final T alter = potentialAlters.remove( random.nextInt( potentialAlters.size() ) );
				final double prob = calcAcceptanceProbability(
						utilityFunction.calcTieUtility( ego , alter ),
						thresholds.getPrimaryTieThreshold() );

				if ( random.nextDouble() < prob ) {
					network.addTie( ego.getId() , alter.getId() );
				}
			}
		}
		counter.printCounter();
	}

	private void fillInSecondaryTies(
			final Random random,
			final SocialNetwork primaryNetwork,
			final SocialNetwork network,
			final SocialPopulation<T> population,
			final long maxNTies) throws SecondaryTieLimitExceededException {
		final Map<Id, T> remainingAgents = new LinkedHashMap<Id, T>( population.getAgentsMap() );

		// we do not need here to re-shuffle the list of agents over and over
		// (we always consider all remaining agents), so contrary to the primary case,
		// we can just fix the order beforehand.
		final List<Id> randomlyOrderedIds = new ArrayList<Id>( remainingAgents.keySet() );
		Collections.shuffle( randomlyOrderedIds , random );

		final Counter counter = new Counter( "consider secondary pair # " );
		long nTies = 0;
		for ( Id id : randomlyOrderedIds ) {
			final T ego = remainingAgents.remove( id );

			final List<T> potentialAlters =
				getUnknownFriendsOfFriends( ego , primaryNetwork , remainingAgents );

			for ( int remainingChecks = (int) (potentialAlters.size() / ((double) stepSizeSecondary));
					remainingChecks > 0 && !potentialAlters.isEmpty();
					remainingChecks--) {
				counter.incCounter();
				final T alter = potentialAlters.remove( random.nextInt( potentialAlters.size() ) );

				final double util = utilityFunction.calcTieUtility( ego , alter );

				final double probKnowingNoPrimary = calcAcceptanceProbability(
						util,
						thresholds.getSecondaryTieThreshold() );

				final double probPrimary = calcAcceptanceProbability(
						util,
						thresholds.getPrimaryTieThreshold() );

				// correction: we want the probability to be friends *knowing that the
				// two individuals are not friends in the abscence of a common friend*
				final double prob = ( probKnowingNoPrimary - probPrimary ) /
					( 1d - probPrimary );

				assert prob >= 0 : prob;
				assert prob <= 1 : prob;

				if ( random.nextDouble() < prob ) {
					network.addTie( ego.getId() , alter.getId() );
					if ( nTies++ > maxNTies ) throw new SecondaryTieLimitExceededException( network );
				}
			}
		}
		counter.printCounter();
	}

	// package visible for tests
	final static <T extends Agent> List<T> getUnknownFriendsOfFriends(
			final T ego,
			final SocialNetwork network,
			final Map<Id, T> remainingAgents) {
		final Set<T> unknownFriendsOfFriends = Collections.newSetFromMap( new LinkedHashMap<T, Boolean>() );
		final Set<Id> alters = network.getAlters( ego.getId() );

		for ( Id alter : alters ) {
			final Set<Id> altersOfAlter = network.getAlters( alter );
			
			for ( Id alterOfAlter : altersOfAlter ) {
				// is the ego?
				if ( alterOfAlter.equals( ego.getId() ) ) continue;
				// already a friend?
				if ( alters.contains( alterOfAlter ) ) continue;
				final T friendOfFriend = remainingAgents.get( alterOfAlter );

				// already allocated?
				if ( friendOfFriend == null ) continue;
				unknownFriendsOfFriends.add( friendOfFriend );
			}
		}

		return new ArrayList<T>( unknownFriendsOfFriends );
	}

	private static double calcAcceptanceProbability(
			final double utility,
			final double threshold) {
		final double expUtility = Math.exp( utility );
		final double expThreshold = Math.exp( threshold );

		if ( (expThreshold + expUtility) == Double.MAX_VALUE ) throw new RuntimeException( "overflow" );

		return expUtility / (expThreshold + expUtility);
	}

	public static class SecondaryTieLimitExceededException extends Exception {
		private static final long serialVersionUID = -3209753011944061999L;
		private final SocialNetwork sn;

		public SecondaryTieLimitExceededException(final SocialNetwork sn) {
			this.sn = sn;
		}

		public SocialNetwork getSocialNetworkAtAbort() {
			return sn;
		}
	}
}

