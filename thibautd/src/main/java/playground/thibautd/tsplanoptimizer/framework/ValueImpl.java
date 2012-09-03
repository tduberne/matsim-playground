/* *********************************************************************** *
 * project: org.matsim.*
 * ValueImpl.java
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
package playground.thibautd.tsplanoptimizer.framework;

/**
 * Default implementation of a {@link Value}. 
 *
 * @author thibautd
 */
public class ValueImpl implements Value {
	private Object value;

	public ValueImpl(final Object value) {
		this.value = value;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public Object setValue(final Object newValue) {
		Object old = value;
		this.value = newValue;
		return old;
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof Value) {
			Object otherValue = ((Value) other).getValue();
			return value.equals( otherValue );
		}

		return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public Value createClone() {
		return new ValueImpl( value );
	}

	@Override
	public String toString() {
		return "ValueImpl<"+value.getClass().getSimpleName()+">="+value;
	}
}

