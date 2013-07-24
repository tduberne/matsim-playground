/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingIO.java
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
package playground.thibautd.hitchiking;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;

/**
 * Reads and writes hitch hiking locations in a simple data format
 * @author thibautd
 */
public class HitchHikingUtils {
	private HitchHikingUtils() {}

	public static Collection<Id> readFile( final String fileName ) {
		BufferedReader reader = IOUtils.getBufferedReader( fileName );
		List<Id> spots = new ArrayList<Id>();

		try {
			Counter counter = new Counter( "reading hitch hiking spot # " );
			for (String line = reader.readLine();
					line != null;
					line = reader.readLine()) {
				counter.incCounter();
				spots.add( new IdImpl( line.trim() ) );
			}
			counter.printCounter();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}

		return spots;
	}

	public static void writeFile(
			final HitchHikingSpots spots,
			final String fileName ) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter( fileName );

			Counter counter = new Counter( "writing hitch hiking spot # " );
			boolean firstLine = true;
			for (Link l : spots.getSpots()) {
				if (firstLine) {
					firstLine = false;
				}
				else {
					writer.newLine();
				}
				counter.incCounter();
				writer.write( l.getId().toString() );
			}
			counter.printCounter();

			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	public static void loadConfig( final Config config , final String file ) {
		config.addModule( new HitchHikingConfigGroup() );
		ConfigUtils.loadConfig ( config , file );
	}

	public static Scenario loadScenario( final Config config ) {
		Scenario sc = ScenarioUtils.loadScenario( config );

		String inFileName = getConfigGroup( config ).getSpotsFile();

		HitchHikingSpots spots =
			new HitchHikingSpots(
					readFile( inFileName ),
					sc.getNetwork() );

		sc.addScenarioElement( spots );

		return sc;
	}

	public static HitchHikingSpots getSpots(final Scenario sc) {
		return sc.getScenarioElement( HitchHikingSpots.class );
	}

	public static HitchHikingConfigGroup getConfigGroup(final Config config) {
		return (HitchHikingConfigGroup) config.getModule(HitchHikingConfigGroup.GROUP_NAME);
	}
}

