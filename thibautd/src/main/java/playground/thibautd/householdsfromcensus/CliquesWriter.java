/* *********************************************************************** *
 * project: org.matsim.*
 * CliquesWriter.java
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
package playground.thibautd.householdsfromcensus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import org.matsim.contrib.socnetsim.framework.cliques.Clique;

/**
 * Writes clique pertenancy information to an XML file.
 * @author thibautd
 */
public class CliquesWriter extends MatsimXmlWriter {
	private static final Logger log =
		Logger.getLogger(CliquesWriter.class);


	private final Map<Id<Clique>, List<Id<Person>>> cliques;
	private final List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
	private int count = 0;
	private int nextLog = 1;

	public CliquesWriter(Map<Id<Clique>, List<Id<Person>>> cliques) {
		this.cliques = cliques;
	}

	public void writeFile(String fileName) {
		this.openFile(fileName);
		this.writeXmlHead();
		this.writeCliques();
		this.close();

		log.info(count+" cliques succesfully dumped to "+fileName);
		count = 0;
		nextLog = 1;
	}

	private void writeCliques() {
		this.atts.clear();

		//this.atts.add(this.createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		this.writeStartTag(CliquesSchemaNames.CLIQUES, this.atts);

		for (Id<Clique> id: this.cliques.keySet()) {
			this.writeClique(id);
		}
		this.writeEndTag(CliquesSchemaNames.CLIQUES);
	}

	private void writeClique(Id<Clique> id) {
		this.logCount();
		this.atts.clear();
		this.atts.add(this.createTuple(CliquesSchemaNames.CLIQUE_ID, id.toString()));
		this.writeStartTag(CliquesSchemaNames.CLIQUE, atts);

		this.writeMembers(this.cliques.get(id));

		this.writeEndTag(CliquesSchemaNames.CLIQUE);
	}

	private void logCount() {
		count++;
		if (count == nextLog) {
			log.info("dumping clique # "+count);
			nextLog *= 2;
		}
	}

	private void writeMembers(List<Id<Person>> clique) {
		for (Id<Person> id : clique) {
			this.atts.clear();
			this.atts.add(this.createTuple(CliquesSchemaNames.MEMBER_ID, id.toString()));
			this.writeStartTag(CliquesSchemaNames.MEMBER, atts, true);
		}
	}
}

