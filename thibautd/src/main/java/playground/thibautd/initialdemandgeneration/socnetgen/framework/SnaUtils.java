/* *********************************************************************** *
 * project: org.matsim.*
 * SnaUtils.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkImpl;
import org.matsim.contrib.socnetsim.utils.CollectionUtils;
import org.matsim.core.router.priorityqueue.BinaryMinHeap;
import org.matsim.core.router.priorityqueue.HasIndex;
import org.matsim.core.router.priorityqueue.MinHeap;
import org.matsim.core.utils.misc.Counter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Provides methods to produce standard statistics for social networks
 * @author thibautd
 */
public class SnaUtils {
	private static final Logger log =
		Logger.getLogger(SnaUtils.class);

	private SnaUtils() {}

	/**
	 * Uses wedge-sampling to estimate clustering coefficient.
	 * see http://onlinelibrary.wiley.com/doi/10.1002/sam.11224/full
	 *
	 * @param precision the result is guaranteed to fall at least so close to the exact value
	 * with probability probabilityPrecision
	 * @param probabilityPrecision the probability with which the precision criterion is fulfilled
	 */
	public static double estimateClusteringCoefficient(
			final long randomSeed,
			final int nThreads,
			final double precision,
			final double probabilityPrecision,
			final SocialNetwork socialNetwork) {
		if ( probabilityPrecision < 0 || probabilityPrecision > 1 ) throw new IllegalArgumentException( "illegal probability "+probabilityPrecision );
		if ( !socialNetwork.isReflective() ) throw new IllegalArgumentException( "cannot estimate clustering on non reflective network" );


		final int k = (int) Math.ceil( 0.5d * Math.pow( precision , -2 ) * Math.log( 2d / (1 - probabilityPrecision) ) );

		if ( socialNetwork instanceof SocialNetworkImpl && ((SocialNetworkImpl) socialNetwork).nTies() < k ) {
			log.info( "social network has less ties than required sample size for approximation: use exact calculation" );
			return calcClusteringCoefficient( socialNetwork );
		}

		final List<Id<Person>> egos = new ArrayList< >( socialNetwork.getEgos().size() );
		final long[] cumulatedWeight = new long[ socialNetwork.getEgos().size() ];
		log.info( "estimating clustering for precision "+precision+" with probability "+probabilityPrecision+":" );
		log.info( "sampling "+k+" wedges for social network with "+socialNetwork.getEgos().size()+" egos" );

		final Counter egoCounter = new Counter( "compute weight of ego # " );
		for ( Map.Entry< Id<Person> , Set<Id<Person>> > ego : socialNetwork.getMapRepresentation().entrySet() ) {
			egoCounter.incCounter();
			egos.add( ego.getKey() );
			final int deg = ego.getValue().size();
			final long prev = egos.size() == 1 ? 0 : cumulatedWeight[ egos.size() - 2 ];
			cumulatedWeight[ egos.size() - 1 ] = prev +( deg * (deg - 1) / 2 );
		}
		egoCounter.printCounter();
		final long sum = cumulatedWeight[ cumulatedWeight.length - 1 ];

		final ExecutorService executor = Executors.newFixedThreadPool( nThreads );

		final List<Future<Integer>> results = new ArrayList< >();

		final Counter wedgeCounter = new Counter( "evaluate wedge # " );
		final int nCallables = nThreads * 4;
		for ( int call=0; call < nCallables; call++ ) {
			final int nSamples =
				(call + 2) * ( k / nCallables ) < k ?
					k / nCallables :
					k - ( nCallables - 1 ) * ( k / nCallables );

			final Random random = new Random( randomSeed + call );
			results.add(
					executor.submit(
					new Callable<Integer>() {
						@Override
						public Integer call() {
							final List<Id<Person>> alters = new ArrayList< >( );
							int nTriangles = 0;

							for ( int i=0; i < nSamples; ) {
								final long sampledWeight = (long) (random.nextDouble() * sum);
								final int ins = Arrays.binarySearch( cumulatedWeight , sampledWeight );
								final int index = ins >= 0 ? ins : -ins - 1;

								final Id<Person> ego = egos.get( index );

								alters.clear();
								alters.addAll( socialNetwork.getAlters( ego ) );

								if ( alters.size() < 2 ) continue;
								i++;
								wedgeCounter.incCounter();

								final Id<Person> alters1 = alters.remove( random.nextInt( alters.size() ) );
								final Id<Person> alters2 = alters.remove( random.nextInt( alters.size() ) );

								if ( socialNetwork.getAlters( alters1 ).contains( alters2 ) ) nTriangles++;
							}

							return nTriangles;
						}
					} ) );
		}

		int nTriangles = 0;
		try {
			for ( Future<Integer> result : results ) {
				nTriangles += result.get();
			}
		}
		catch ( InterruptedException | ExecutionException e ) {
			// avoid wrapping a wrapper exception...
			if ( e.getCause() instanceof RuntimeException ) throw (RuntimeException) e.getCause();
			else if ( e.getCause() != null ) throw new RuntimeException( e.getCause() );
			throw new RuntimeException( e );
		}
		wedgeCounter.printCounter();

		executor.shutdown();

		return ((double) nTriangles) / k;
	}

	public static double calcClusteringCoefficient(
			final SocialNetwork socialNetwork) {
		log.info( "compute clustering coefficient with full enumeration" );
		final Counter tripleCounter = new Counter( "clustering calculation: look at triple # " );
		long nTriples = 0;
		long nTriangles = 0;

		for ( Id<Person> ego : socialNetwork.getEgos() ) {
			final Set<Id<Person>> alterSet = socialNetwork.getAlters( ego );
			final Id<Person>[] alters = alterSet.toArray( new Id[ alterSet.size() ] ); 

			for ( int alter1index = 0; alter1index < alters.length; alter1index++ ) {
				final Set<Id<Person>> altersOfAlter1 = socialNetwork.getAlters( alters[ alter1index ] );
				for ( int alter2index = alter1index + 1; alter2index < alters.length; alter2index++ ) {
					// this is a new triple
					tripleCounter.incCounter();
					nTriples++;

					if ( altersOfAlter1.contains( alters[ alter2index ] ) ) {
						nTriangles++;
					}
				}
			}
		}
		tripleCounter.printCounter();

		// note: in Arentze's paper, it is 3 * triangle / triples.
		// but here, we count every triangle three times.
		assert nTriples >= 0 : nTriples;
		assert nTriangles >= 0 : nTriangles;
		return nTriples > 0 ? (1d * nTriangles) / nTriples : 0;
	}

	public static double calcAveragePersonalNetworkSize(final SocialNetwork socialNetwork) {
		int count = 0;
		long sum = 0;
		for ( Id ego : socialNetwork.getEgos() ) {
			count++;
			sum += socialNetwork.getAlters( ego ).size();
		}
		return ((double) sum) / count;
	}

	public static Collection<Set<Id<Person>>> identifyConnectedComponents(
			final SocialNetwork sn) {
		if ( !sn.isReflective() ) {
			throw new IllegalArgumentException( "the algorithm is valid only with reflective networks" );
		}
		final Map<Id<Person>, Set<Id<Person>>> altersMap = new LinkedHashMap<>( sn.getMapRepresentation() );
		final Collection< Set<Id<Person>> > components = new ArrayList<>();
	
		while ( !altersMap.isEmpty() ) {
			// DFS implemented as a loop (recursion results in a stackoverflow on
			// big networks)
			final Id<Person> seed = CollectionUtils.getElement( 0 , altersMap.keySet() );
	
			final Set<Id<Person>> component = new HashSet<>();
			components.add( component );
			component.add( seed );
	
			final Queue<Id<Person>> stack = Collections.asLifoQueue( new ArrayDeque<Id<Person>>( altersMap.size() ) );
			stack.add( seed );
	
			while ( !stack.isEmpty() ) {
				final Id<Person> current = stack.remove();
				final Set<Id<Person>> alters = altersMap.remove( current );
	
				for ( Id<Person> alter : alters ) {
					if ( component.add( alter ) ) {
						stack.add( alter );
					}
				}
			}
	
		}
	
		return components;
	}

	public static void sampleSocialDistances(
			final SocialNetwork socialNetwork,
			final Random random,
			final int nPairs,
			final DistanceCallback callback  ) {

		log.info( "searching for the biggest connected component..." );
		PairRandomizer biggestComponent = null;
		for ( Set<Id<Person>> component : SnaUtils.identifyConnectedComponents( socialNetwork ) ) {
			if ( biggestComponent == null || biggestComponent.ids.size() < component.size() ) {
				biggestComponent = new PairRandomizer( random , component );
			}
		}

		log.info( "considering only biggest component with size "+ biggestComponent.ids.size() );
		log.info( "ignoring "+( socialNetwork.getEgos().size() - biggestComponent.ids.size() )+" agents ("+
				( ( socialNetwork.getEgos().size() - biggestComponent.ids.size() ) * 100d / socialNetwork.getEgos().size() )+"%)" );


		final Counter counter = new Counter( "sampling pair # " );
		for ( int i = 0; i < nPairs; i++ ) {
			counter.incCounter();
			final Id<Person> ego = biggestComponent.next();
			final Id<Person> alter = biggestComponent.next();

			callback.notifyDistance(
					ego, alter ,
					calcNHops( socialNetwork , ego , alter ) );
		}
		counter.printCounter();
	}

	// "two way dijkstra" directly on the social network.
	// given the small world property, this should be much faster than standard dijkstra.
	// this also avoids creating a matsim network to run the standard dijkstra, saving precious memory
	// it is, in practice, roughly 50 times faster than using MATSim's dijkstra on an equivalent matsim network.
	// No idea if this comes from the 2-way (it might be, given the small-world property) or simply the implementation...
	private static int calcNHops( final SocialNetwork network , final Id<Person> ego, final Id<Person> alter ) {
		// does this implementation really speeds things up? It takes quite a lot of space, as it does not resizes...
		final MinHeap<TaggedId> queue = new BinaryMinHeap<>( network.getEgos().size() );
		final TaggedIdPool pool = new TaggedIdPool();

		queue.add( pool.getTaggedId( ego , true , 0 ) , 0 );
		queue.add( pool.getTaggedId( alter , false , 0 ) , 0 );

		while ( !queue.isEmpty() ) {
			final TaggedId current = queue.poll();

			final int newDist = current.dist + 1;

			final Set<Id<Person>> currentAlters = network.getAlters( current.id );
			for ( Id<Person> currentAlter : currentAlters ) {
				final TaggedId taggedId = pool.getTaggedId( currentAlter , current.fromEgo , newDist );

				// we joined the two trees
				if ( taggedId.fromEgo == !current.fromEgo ) return newDist + taggedId.dist;
				if ( taggedId.dist == newDist ) {
					// it was newly created
					queue.add( taggedId , taggedId.dist );
				}
			}
		}

		throw new RuntimeException();
	}

	private static class TaggedIdPool {
		private int index = 0;

		private final Map<Id<Person>, TaggedId> ids = new HashMap<>();

		public TaggedId getTaggedId( final Id<Person> id , final boolean fromEgo , final int dist ) {
			TaggedId taggedId = ids.get( id );
			if ( taggedId == null ) {
				taggedId = new TaggedId( id , index++, fromEgo, dist );
				ids.put( id , taggedId );
			}

			return taggedId;
		}
	}
	private static class TaggedId implements HasIndex {
		final Id<Person> id;
		final int index;

		final boolean fromEgo;
		final int dist;

		private TaggedId( final Id<Person> id, final int index, final boolean fromEgo, final int dist ) {
			this.id = id;
			this.index = index;
			this.fromEgo = fromEgo;
			this.dist = dist;
		}

		@Override
		public int getArrayIndex() {
			return index;
		}
	}

	@FunctionalInterface
	public interface DistanceCallback {
		void notifyDistance( final Id<Person> ego , final Id<Person> alter , final double distance );
	}

	private static class PairRandomizer {
		private final List<Id<Person>> ids;
		private int index = 0;
		private int step = 1;

		private PairRandomizer(
				final Random random,
				final Collection<Id<Person>> ids ) {
			this.ids = new ArrayList<>( ids );
			Collections.shuffle( this.ids , random );
		}

		public Id<Person> next() {
			final Id<Person> id = ids.get( index );

			index += step;
			if ( index >= ids.size() ) {
				index = 0;
				step++;
			}

			return id;
		}
	}
}

