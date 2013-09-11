/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractPersonEvent.java
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
package eu.eunoiaproject.bikesharing.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.internal.HasPersonId;

/**
 * @author thibautd
 */
abstract class AbstractPersonEvent extends Event implements HasPersonId {
	private final Id personId;

	public AbstractPersonEvent(
			final double time,
			final Id personId) {
		super( time );
		this.personId = personId;
	}

	@Override
	public Id getPersonId() {
		return personId;
	}

	@Override
	public Map<String, String> getAttributes() {
		final Map<String, String> map = super.getAttributes();
		map.put( "person" , personId.toString() );
		return map;
	}
}
