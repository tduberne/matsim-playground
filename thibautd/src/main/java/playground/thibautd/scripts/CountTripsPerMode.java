/* *********************************************************************** *
 * project: org.matsim.*
 * CountTripsPerMode.java
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
package playground.thibautd.scripts;

import java.io.BufferedWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.PtConstants;
import org.xml.sax.Attributes;

import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.parknride.ParkAndRideConstants;

/**
 * @author thibautd
 */
public class CountTripsPerMode {
	private static String PLAN = "plan";
	private static String SEL = "selected";
	private static String ACT = "act";
	private static String ACTTYPE = "type";
	private static String MODE = "mode";
	private static String LEG = "leg";

	public static void main(final String[] args) {
		String plansFile = args[ 0 ];
		String outFile = args[ 1 ];

		PlanParser parser = new PlanParser();
		parser.parse( plansFile );

		BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
		try {
			writer.write( "mode\tcount\tshare" );

			Counter counter = new Counter( "writing line # " );
			double total = parser.totalCount;
			for (Map.Entry<String, Count> c : parser.counts.entrySet()) {
				counter.incCounter();
				writer.newLine();
				int count = c.getValue().getCount();
				writer.write( c.getKey()+"\t"+count+"\t"+(count / total)  );
			}
			counter.printCounter();

			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private static class PlanParser extends MatsimXmlParser {
		private boolean isSelectedPlan = false;
		private String currentMode = null;
		int totalCount = 0;
		final Map<String, Count> counts = new HashMap<String, Count>();
		private final Counter counter = new Counter( "reading plan # " );
		private boolean isFirstAct = true;
		private boolean isPnr = false;
		private boolean isPt = false;
		private boolean isJoint = false;

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if ( PLAN.equals( name ) ) {
				if ( "yes".equals( atts.getValue( SEL ) ) ) {
					counter.incCounter();
					isSelectedPlan = true;
					isFirstAct = true;
				}
				else {
					isSelectedPlan = false;
				}
			}
			else if (isSelectedPlan) {
				if ( ACT.equals( name ) ) {
					handleAct( atts );
				}
				else if ( LEG.equals( name ) ) {
					handleLeg( atts );
				}
			}
		}

		private void handleAct(final Attributes atts) {
			if (isFirstAct) {
				isFirstAct = false;
				return;
			}

			String type = atts.getValue( ACTTYPE );

			if ( type.equals( PtConstants.TRANSIT_ACTIVITY_TYPE ) ) {
				isPt = true;
			}
			else if ( type.equals( ParkAndRideConstants.PARKING_ACT ) ) {
				isPnr = true;
			}
			else if (type.equals( JointActingTypes.PICK_UP ) || type.equals( JointActingTypes.DROP_OFF )) {
				isJoint = true;
			}
			else {
				// "hierarchy" of modes (p+r is pt as well, pt is normally marked as transit_walk)
				if (!HitchHikingConstants.PASSENGER_MODE.equals( currentMode ) &&
					!HitchHikingConstants.DRIVER_MODE.equals( currentMode ) ) {
					currentMode = isJoint ? "shared ride" : (isPnr ? "P+R" : (isPt ? TransportMode.pt : currentMode));
				}
				Count c = counts.get( currentMode );

				if (c == null) {
					c = new Count();
					counts.put( currentMode , c );
				}

				c.inc();
				totalCount++;
				currentMode = null;
				isPnr = false;
				isPt = false;
				isJoint = false;
			}
		}

		private void handleLeg(final Attributes atts) {
			if (!HitchHikingConstants.PASSENGER_MODE.equals( currentMode ) &&
				!HitchHikingConstants.DRIVER_MODE.equals( currentMode ) ) {
				currentMode = atts.getValue( MODE );
			}
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
		}
	}

	private static class Count {
		private int c = 0;

		public void inc() {
			c++;
		}

		public int getCount() {
			return c;
		}
	}
}

