/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerStartsWaitingEvent.java
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
package playground.thibautd.hitchiking.qsim.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEvent;
import org.matsim.core.events.LinkEventImpl;
import org.matsim.core.events.PersonEventImpl;

/**
 * @author thibautd
 */
public abstract class WaitingEvent extends PersonEventImpl implements LinkEvent {
	private final Id link;

	protected WaitingEvent(
			final double time,
			final Id agentId,
			final Id linkId) {
		super( time , agentId );
		link = linkId;
	}

	@Override
	public Id getLinkId() {
		return link;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> atts = super.getAttributes();

		atts.put( LinkEventImpl.ATTRIBUTE_LINK  , ""+link );

		return atts;
	}
}

