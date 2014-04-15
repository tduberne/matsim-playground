/* *********************************************************************** *
 * project: org.matsim.*
 * Adress.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.geocoding;

/**
 * @author thibautd
 */
public class Address {
	// note: this is not final, because otherwise it leads to a confusing constructor
	private String street;
	private String number;
	private String zipcode;
	private String municipality;
	private String country;
	
	public String getStreet() {
		return street;
	}
	public String getNumber() {
		return number;
	}
	public String getZipcode() {
		return zipcode;
	}
	public String getMunicipality() {
		return municipality;
	}
	public String getCountry() {
		return country;
	}
	public void setStreet(String street) {
		this.street = street;
	}
	public void setNumber(String number) {
		this.number = number;
	}
	public void setZipcode(String zipcode) {
		this.zipcode = zipcode;
	}
	public void setMunicipality(String municipality) {
		this.municipality = municipality;
	}
	public void setCountry(String country) {
		this.country = country;
	}
}

