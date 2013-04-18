/* *********************************************************************** *
 * project: org.matsim.*
 * DumpSizeOfSelectedJointPlans.java
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
package playground.thibautd.scripts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

/**
 * @author thibautd
 */
public class DumpSizeOfSelectedJointPlans {
	public static void main(final String[] args) throws IOException {
		final String plansFile = args[ 0 ];
		final String jointPlansFile = args[ 1 ];
		final String outFile = args[ 2 ];

		final Map<Id, Integer> selectedIndices = parseSelectedIndices( plansFile );
		final Set<Id> agentWithNoJointPlan = new HashSet<Id>( selectedIndices.keySet() );

		final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
		writer.write( "personId\tsize" );

		final Counter counter = new Counter( "parse joint plan # " );
		new MatsimXmlParser( false ) {
			int size = -1;
			boolean isSelected = false;
			final List<Id> persons = new ArrayList<Id>();
			@Override
			public void startTag(
					final String name,
					final Attributes atts,
					final Stack<String> context) {
				if ( name.equals( "jointPlan" ) ) {
					counter.incCounter();
					size = 0;
					isSelected = false;
					persons.clear();
				}
				if ( name.equals( "individualPlan" ) ) {
					size++;
					final Id id = new IdImpl(
									atts.getValue(
										"personId" ).trim() );
					final Integer selectedIndex = selectedIndices.get( id );
					persons.add( id );
					if ( selectedIndex.equals(
									Integer.valueOf(
										atts.getValue(
											"planNr" ) ) ) ) {
						if ( size > 1 && !isSelected ) throw new RuntimeException();
						isSelected = true;
					}
				}
			}

			@Override
			public void endTag(
					final String name,
					final String content,
					final Stack<String> context) {
				if ( name.equals( "jointPlan" ) && isSelected ) {
					agentWithNoJointPlan.removeAll( persons );
					try {
						for ( Id p : persons ) {
							writer.newLine();
							writer.write( p+"\t"+size );
						}
					}
					catch (IOException e) {
						throw new UncheckedIOException( e );
					}
				}
			}
		}.parse( jointPlansFile );

		for ( Id p : agentWithNoJointPlan ) {
			writer.newLine();
			writer.write( p+"\t"+1 );
		}

		counter.printCounter();
		writer.close();
	}

	private static Map<Id, Integer> parseSelectedIndices(
			final String plansFile) {
		final Map<Id, Integer> map = new HashMap<Id, Integer>();

		final Counter counter = new Counter( "parse person # " );
		new MatsimXmlParser() {
			private Id currentAgent = null;
			private int currIndex = -1;

			@Override
			public void startTag(
					final String name,
					final Attributes atts,
					final Stack<String> context) {
				if ( name.equals( "person" ) ) {
					counter.incCounter();
					currentAgent = new IdImpl( atts.getValue( "id" ).trim() );
					currIndex = -1;
				}
				if ( name.equals( "plan" ) ) {
					currIndex++;
					if ( atts.getValue( "selected" ).equals( "yes" ) ) {
						final Integer old = map.put( currentAgent , currIndex );
						if ( old != null ) throw new RuntimeException();
					}
				}
			}

			@Override
			public void endTag(
					final String name,
					final String content,
					final Stack<String> context) {
			}
		}.parse( plansFile );

		counter.printCounter();

		return map;
	}
}
