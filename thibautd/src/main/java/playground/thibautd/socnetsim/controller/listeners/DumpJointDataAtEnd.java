/* *********************************************************************** *
 * project: org.matsim.*
 * DumpJointDataAtEnd.java
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
package playground.thibautd.socnetsim.controller.listeners;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import playground.thibautd.socnetsim.population.JointPlansXmlWriter;

/**
 * @author thibautd
 */
public class DumpJointDataAtEnd implements ShutdownListener {
	private final DumpDataAtEnd individualDumper;
	private final Scenario scenario;
	private final OutputDirectoryHierarchy controlerIO;

	public DumpJointDataAtEnd(
			final Scenario scenarioData,
			final OutputDirectoryHierarchy controlerIO) {
		this.individualDumper = new DumpDataAtEnd( scenarioData , controlerIO );
		this.scenario = scenarioData;
		this.controlerIO = controlerIO;
	}

	@Override
	public void notifyShutdown(final ShutdownEvent event) {
		individualDumper.notifyShutdown( event );
		dumpJointPlans();
	}

	private void dumpJointPlans() {
		JointPlansXmlWriter.write(
				scenario.getPopulation(),
				controlerIO.getOutputFilename( "output_jointPlans.xml.gz" ) );
	}
}

