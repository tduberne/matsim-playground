/* *********************************************************************** *
 * project: org.matsim.*
 * ReducedSPModelDecisionMakerFactory.java
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
package playground.thibautd.agentsmating.logitbasedmating.spbasedmodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;

import playground.thibautd.agentsmating.logitbasedmating.basic.DecisionMakerImpl;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMaker;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMakerFactory;
import playground.thibautd.agentsmating.logitbasedmating.spbasedmodel.populationenrichingmodels.SpeaksGermanModel;
import playground.thibautd.agentsmating.logitbasedmating.spbasedmodel.populationenrichingmodels.TravelCardModel;
import playground.thibautd.agentsmating.logitbasedmating.spbasedmodel.populationenrichingmodels.TravelCardModel.TravelCard;

/**
 * @author thibautd
 */
public class ReducedSPModelDecisionMakerFactory implements DecisionMakerFactory {

	private final TravelCardModel travelCardModel = new TravelCardModel();
	private final SpeaksGermanModel speaksGermanModel = new SpeaksGermanModel();

	@Override
	public DecisionMaker createDecisionMaker(final Person agent) throws UnelectableAgentException {
		PersonImpl pImpl;

		try {
			pImpl = (PersonImpl) agent;
		} catch (ClassCastException e) {
			throw new RuntimeException("can only handle PersonImpl agents", e);
		};

		return createDecisionMaker( pImpl );
	}

	public DecisionMaker createDecisionMaker(final PersonImpl agent) throws UnelectableAgentException {
		checkEligible( agent );

		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put(
				ReducedModelConstants.A_AGE ,
				agent.getAge() );
		attributes.put(
				ReducedModelConstants.A_IS_MALE ,
				agent.getSex().matches("m.*") );

		Collection<TravelCard> cards = travelCardModel.getTravelCards( agent );
		attributes.put(
				ReducedModelConstants.A_HAS_GENERAL_ABO ,
				cards.contains( TravelCard.GENERAL_ABONNEMENT ) );

		attributes.put(
				ReducedModelConstants.A_HAS_HALBTAX ,
				cards.contains( TravelCard.HALBTAX ) );
		attributes.put(
				ReducedModelConstants.A_SPEAKS_GERMAN ,
				speaksGermanModel.speaksGerman( agent ) );
		attributes.put(
				ReducedModelConstants.A_IS_CAR_ALWAYS_AVAIL ,
				agent.getCarAvail().equals("always") );

		return new DecisionMakerImpl( agent.getId() , attributes );
	}

	private void checkEligible(final PersonImpl agent) throws UnelectableAgentException {
		if ( !agent.hasLicense() ) {
			throw new UnelectableAgentException("agent has no driving license");
		}
		if ( agent.getCarAvail().equals("never") ) {
			throw new UnelectableAgentException("agent has a driving license but no car");
		}
	}
}

