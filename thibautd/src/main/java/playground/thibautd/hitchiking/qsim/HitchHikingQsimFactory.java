/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingQsimFactory.java
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
package playground.thibautd.hitchiking.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;

import playground.thibautd.hitchiking.HitchHikingConstants;

/**
 * @author thibautd
 */
public class HitchHikingQsimFactory implements MobsimFactory {

    private final static Logger log = Logger.getLogger(QSimFactory.class);

	private final Controler controler;

	public HitchHikingQsimFactory(final Controler controler) {
		this.controler = controler;
	}

    @Override
    public Netsim createMobsim(
			final Scenario sc,
			EventsManager eventsManager) {

        QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }

		// make sure we simulate car pooling!
		Collection<String> mainModes = conf.getMainModes();
		if (!mainModes.contains( HitchHikingConstants.DRIVER_MODE )) {
			List<String> ms = new ArrayList<String>(mainModes);
			ms.add( HitchHikingConstants.DRIVER_MODE );
			conf.setMainModes( ms );
		}

        // Get number of parallel Threads
        int numOfThreads = conf.getNumberOfThreads();
        QNetsimEngineFactory netsimEngFactory;
        if (numOfThreads > 1) {
            eventsManager = new SynchronizedEventsManagerImpl(eventsManager);
            netsimEngFactory = new ParallelQNetsimEngineFactory();
            log.info("Using parallel QSim with " + numOfThreads + " threads.");
        }
		else {
            netsimEngFactory = new DefaultQSimEngineFactory();
        }

        //QSim qSim = QSim.createQSimWithDefaultEngines(sc, eventsManager, netsimEngFactory);

		// default initialisation
		QSim qSim = new QSim(sc,  eventsManager );
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);

		// set specific engine
		PassengerQueuesManager queuesManager = new PassengerQueuesManager( eventsManager );
		qSim.addMobsimEngine( queuesManager );
		qSim.addDepartureHandler( queuesManager );
        AgentFactory agentFactory =
			new HitchHikerAgentFactory(
					new TransitAgentFactory(qSim),
					controler.getNetwork(),
					controler.getTripRouterFactory().instantiateAndConfigureTripRouter(),
					queuesManager,
					eventsManager,
					controler.getConfig().planCalcScore().getMonetaryDistanceCostRateCar());

        if (sc.getConfig().scenario().isUseTransit()) {
            TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
            transitEngine.setUseUmlaeufe(true);
            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
            qSim.addDepartureHandler(transitEngine);
            qSim.addAgentSource(transitEngine);
            qSim.addMobsimEngine(transitEngine);
        }
        
        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);
        return qSim;
    }

}
