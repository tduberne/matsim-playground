/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package herbie.creation.freight;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class Zone {
	private Id id;
	private Coord centroidCoord;
	private String name;
	
	public Zone(Id id, Coord centroidCoord, String name) {
		this.id = id;
		this.centroidCoord = centroidCoord;
		this.name = name;
	}
	
	public Id getId() {
		return id;
	}
	public void setId(Id id) {
		this.id = id;
	}
	public Coord getCentroidCoord() {
		return centroidCoord;
	}
	public void setCentroidCoord(Coord centroidCoord) {
		this.centroidCoord = centroidCoord;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
