/* *********************************************************************** *
 * project: org.matsim.*
 * JointActImpl.java
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
package playground.thibautd.jointtrips.population;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.ActivityImpl;

import playground.thibautd.utils.UniqueIdFactory;

/**
 * @author thibautd
 */
public class JointActivity extends ActivityImpl implements Activity, JointActing, Identifiable {
	// must extend ActivityImpl, as there exist parts of the code (mobsim...) where
	// Activities are casted to ActivityImpl.

	private static final UniqueIdFactory idFactory = new UniqueIdFactory( "activity" );
	// joint activity currently unsupported
	private final boolean isJoint = false;
	//private List<Person> participants = null;
	//private Map<Id, JointActivity> linkedActivities = new HashMap<Id, JointActivity>();
	private Person person;
	private final String initialType;
	private final Id id;

	/*
	 * =========================================================================
	 * Constructors
	 * =========================================================================
	 */
	public JointActivity(final String type, final Id linkId, final Person person) {
		//super(type, linkId);
		//this.person = person;
		//this.initialType = type;
		this( type , null , linkId , person );
	}

	public JointActivity(final String type, final Coord coord, final Person person) {
		//super(type, coord);
		//this.person = person;
		//this.initialType = type;
		this( type , coord , null , person );
	}

	public JointActivity(final String type, final Coord coord, final Id linkId, final Person person) {
		super(type, coord, linkId);
		this.person = person;
		this.initialType = type;
		this.id = idFactory.createNextId();
	}

	public JointActivity(final ActivityImpl act, final Person person) {
		super(act);
		this.person = person;
		this.initialType = act.getType();
		this.id = idFactory.createNextId();
	}

	public JointActivity(final JointActivity act) {
		super((ActivityImpl) act);
		constructFromJointActivity(act);
		this.initialType = act.getInitialType();
		this.id = act.getId();
	}

	public JointActivity(Activity act, Person pers) {
		super((ActivityImpl) act);
		if (act instanceof JointActivity) {
			constructFromJointActivity((JointActivity) act);
			this.initialType = ((JointActivity) act).getInitialType();
			this.id = ((JointActivity) act).getId();
		} else {
			this.person = pers;
			this.initialType = act.getType();
			this.id = idFactory.createNextId();
		} 
	}

	private void constructFromJointActivity(final JointActivity act) {
		this.person = act.getPerson();
	}

	/*
	 * =========================================================================
	 * JointActing-specific methods
	 * =========================================================================
	 */

	@Override
	public boolean getJoint() {
		return this.isJoint;
	}

	@Override
	public void setLinkedElements(final Map<Id, ? extends JointActing> linkedElements) {
		this.linkageError();
	}

	@Override
	public void addLinkedElement(
			final Id id,
			final JointActing act) {
		this.linkageError();
	}

	@Override
	public Map<Id, ? extends JointActing> getLinkedElements() {
		this.linkageError();
		return null;
	}

	@Override
	public Person getPerson() {
		return this.person;
	}
	
	@Override
	public void setPerson(final Person person) {
		this.person = person;
	}

	@Override
	public void setLinkedElementsById(final List<? extends Id> linkedElements) {
		this.linkageError();
	}

	@Override
	public void addLinkedElementById(final Id linkedElement) {
		this.linkageError();
	}

	@Override
	public List<? extends Id> getLinkedElementsIds() {
		return null;
	}

	private void linkageError() {
		throw new UnsupportedOperationException("linkage of activities not supported yet");
	}

	@Deprecated
	protected Activity getDelegate() {
		return (ActivityImpl) this;
	}

	/**
	 * Useful for Pick ups, which initial type "encode" the links between them.
	 */
	public String getInitialType() {
		return this.initialType;
	}

	@Override
	public Id getId() {
		return id;
	}

}
