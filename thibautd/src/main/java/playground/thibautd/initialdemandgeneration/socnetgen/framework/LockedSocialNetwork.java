/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetwork.java
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.socnetsim.population.SocialNetwork;

/**
 * @author thibautd
 */
public class LockedSocialNetwork implements SocialNetwork {
	private static final Logger log =
		Logger.getLogger(LockedSocialNetwork.class);

	private final Map<Id, Set<Id<Person>>> alterEgoMap;
	private final boolean failOnUnknownEgo;
	private boolean locked = false;
	
	private final Map<String, String> metadata = new LinkedHashMap<String, String>();


	// mainly to simplify writing tests
	public LockedSocialNetwork( final boolean failOnUnknownEgo ) {
		this( failOnUnknownEgo,
				new LinkedHashMap<Id, Set<Id<Person>>>() );
	}

	private LockedSocialNetwork( final boolean failOnUnknownEgo , final Map<Id, Set<Id<Person>>> map ) {
		this.failOnUnknownEgo = failOnUnknownEgo;
		this.alterEgoMap = map;
	}

	private LockedSocialNetwork(
			final int initialCap,
			final float loadFactor) {
		this( true , new LinkedHashMap<Id, Set<Id<Person>>>( initialCap , loadFactor ) );
	}

	public LockedSocialNetwork() {
		this( true );
	}

	public LockedSocialNetwork(final SocialPopulation<?> pop) {
		this( pop.getAgents() );
	}

	public LockedSocialNetwork(final LockedSocialNetwork toCopy) {
		this( toCopy.failOnUnknownEgo,
			new LinkedHashMap<Id, Set<Id<Person>>>( toCopy.alterEgoMap ) );
	}

	public LockedSocialNetwork(final Collection<? extends Identifiable> egos) {
		// make the map big to avoid collisions
		this( egos.size() * 2 , 0.5f ); 
		addEgosObjects( egos );
	}

	public void lock() {
		this.locked = true;
	}

	public boolean isLocked() {
		return locked;
	}

	public void addTie(final Id id1, final Id id2) {
		if ( locked ) throw new IllegalStateException();
		addAlter( id1 , id2 );
		addAlter( id2 , id1 );
	}

	public void addEgosObjects(final Iterable<? extends Identifiable> egos) {
		if ( locked ) throw new IllegalStateException();
		for ( Identifiable ego : egos ) addEgo( ego.getId() );
	}

	@Override
	public void addEgos(final Iterable<? extends Id> ids) {
		if ( locked ) throw new IllegalStateException();
		for ( Id id : ids ) addEgo( id );
	}

	@Override
	public void addEgo(final Id id) {
		if ( locked ) throw new IllegalStateException();
		if ( !alterEgoMap.containsKey( id ) ) {
			alterEgoMap.put( id , new HashSet<Id<Person>>() );
		}
	}

	private void addAlter(
			final Id ego,
			final Id<Person> alter) {
		if ( locked ) throw new IllegalStateException();
		Set<Id<Person>> alters = alterEgoMap.get( ego );

		if ( alters == null ) {
			if (failOnUnknownEgo) {
				throw new IllegalArgumentException( "ego "+ego+" is unknown" );
			}
			alters = new HashSet<Id<Person>>();
			alterEgoMap.put( ego , alters );
		}

		alters.add( alter );
	}

	@Override
	public Set<Id<Person>> getAlters(final Id ego) {
		final Set<Id<Person>> alters = alterEgoMap.get( ego );

		return alters != null ?
			Collections.unmodifiableSet( alters ) :
			Collections.<Id<Person>>emptySet();
	}

	@Override
	public Set<Id> getEgos() {
		return Collections.unmodifiableSet( alterEgoMap.keySet() );
	}

	private LockedSocialNetwork secondaryNetwork = null;

	public LockedSocialNetwork getNetworkOfUnknownFriendsOfFriends() {
		if ( !locked ) {
			log.warn( "getting the network of secondary ties has the side effect of locking the primary network" );
			lock();
		}
		if ( secondaryNetwork == null ) buildSecondaryNetwork();
		return secondaryNetwork;
	}

	private void buildSecondaryNetwork() {
		final Counter counter = new Counter( "search secondary friends of agent # ");
		secondaryNetwork = new LockedSocialNetwork( getEgos().size() * 2 , 0.5f );
		secondaryNetwork.addEgos( getEgos() );

		for ( Id ego : getEgos() ) {
			final Set<Id<Person>> alters = getAlters( ego );
			counter.incCounter();

			for ( Id alter : alters ) {
				final Set<Id<Person>> altersOfAlter = getAlters( alter );
				
				for ( Id alterOfAlter : altersOfAlter ) {
					// is the ego?
					if ( alterOfAlter.equals( ego ) ) continue;
					// already a friend?
					if ( alters.contains( alterOfAlter ) ) continue;

					secondaryNetwork.addTie( ego , alterOfAlter );
				}
			}
		}
		counter.printCounter();

		secondaryNetwork.lock();
	}

	@Override
	public void addBidirectionalTie(Id id1, Id id2) {
		addTie( id1 , id2 );
	}

	@Override
	public void addMonodirectionalTie(Id ego, Id alter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Id, Set<Id<Person>>> getMapRepresentation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isReflective() {
		return true;
	}

	@Override
	public Map<String, String> getMetadata() {
		return metadata;
	}

	@Override
	public void addMetadata(String att, String value) {
		metadata.put( att , value );
	}
}

