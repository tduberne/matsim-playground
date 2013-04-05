/* *********************************************************************** *
 * project: org.matsim.*
 * MapUtils.java
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
package playground.thibautd.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd
 */
public class MapUtils {
	private MapUtils() {}

	public static <K,V> Collection<V> getCollection(
			final K key,
			final Map<K, Collection<V>> map) {
		Collection<V> coll = map.get( key );

		if ( coll == null ) {
			coll = new ArrayList<V>();
			map.put( key , coll );
		}

		return coll;
	}

	public static <K,V> List<V> getList(
			final K key,
			final Map<K, List<V>> map) {
		List<V> coll = map.get( key );

		if ( coll == null ) {
			coll = new ArrayList<V>();
			map.put( key , coll );
		}

		return coll;
	}

	public static <K,V> Set<V> getSet(
			final K key,
			final Map<K, Set<V>> map) {
		Set<V> coll = map.get( key );

		if ( coll == null ) {
			coll = new HashSet<V>();
			map.put( key , coll );
		}

		return coll;
	}
}

