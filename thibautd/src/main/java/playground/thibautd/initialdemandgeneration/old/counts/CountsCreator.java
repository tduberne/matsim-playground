/* *********************************************************************** *
 * project: org.matsim.*
 * CountsCreator.java
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
package playground.thibautd.initialdemandgeneration.old.counts;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.counts.CountsWriter;

/**
 * @author thibautd
 */
public class CountsCreator {
	private final static Logger log = Logger.getLogger(CountsCreator.class);
	
	private final CountsConfigGroup configGroup;

	public CountsCreator(
			final CountsConfigGroup configGroup) {
		this.configGroup = configGroup;
	}

	public void run() {			
		for (int month = 0; month <= 12; month++) {
			Stations stations = readCounts();			
			this.filtering(stations, month);
			this.writeSummary(stations, month);
			
			if (month == 0) this.writeCountsAndOriginalCounts(stations);
		}	
	}
	
	private Stations readCounts() {
		CountsReaderYear reader = new CountsReaderYear();
		reader.read( configGroup.getDatasetsListFile() );
				
		log.info("Do the cleaning ------------------");
		Cleaner cleaner = new Cleaner();
		TreeMap<String, Vector<RawCount>> rawCounts = cleaner.cleanRawCounts(reader.getRawCounts());
				
		NetworkMapper mapper = new NetworkMapper( configGroup.getRemoveZeroVolumes() );
		mapper.map(rawCounts, configGroup.getNetworkMappingFile() );
		
		Stations stations = new Stations();
		stations.setCountStations(mapper.getCountStations());
		
		return stations;
	}
	
	private void filtering(Stations stations, int monthFilter){
		log.info("Do the filtering ------------------");
		TimeFilter filter = new TimeFilter();
		filter.setDayFilter( configGroup.getDayFilter() );
		filter.setMonthFilter(monthFilter);
		filter.setSummerHolidaysFilter( configGroup.getRemoveSummerHolidays() );
		filter.setXmasDays( configGroup.getRemoveXmasDays() );
		log.info(" 	day filter: " + configGroup.getDayFilter()  + "; month filter: " + monthFilter + "; " + stations.getCountStations().size() + " stations to be filtered");
		
		List<CountStation> emptyStations = new Vector<CountStation>();
		
		Iterator<CountStation> station_it = stations.getCountStations().iterator();
		while (station_it.hasNext()) {
			CountStation station = station_it.next();
			log.info("	Station " + station.getId() + " number of counts before filtering " + station.getCounts().size() + " i.e. days: " + 
					station.getCounts().size()/ 24.0);
			station.filter(filter);	
			station.mapCounts();
			station.aggregate( configGroup.getRemoveOutliers() );
			log.info("	Station " + station.getId() + " number of counts after filtering: " + station.getCounts().size() + " i.e. days: " + 
					station.getCounts().size()/ 24.0);
			log.info("	--- ");
			
			if (station.getCounts().size() == 0) emptyStations.add(station);
		}
		
		// remove empty stations
		log.info("Removing " + emptyStations.size() + " empty stations");
		for (CountStation station : emptyStations) {
			stations.removeCountStation(station);
		}
	}
	
	private void writeSummary(Stations stations, int monthFilter) {
		log.info("Writing the summary -------------------");
		log.info("	Number of stations to write: " + stations.getCountStations().size());
		SummaryWriter summaryWriter = new SummaryWriter();
		
		String outpath = configGroup.getOutputDir();

		log.info(" 		write analysis for specific area: " + false);
		summaryWriter.write(stations, outpath + "/analysis/" + monthFilter + "/", false);
		
		log.info(" 		write analysis for specific area: " + true);
		summaryWriter.write(stations, outpath + "/analysis/" + monthFilter + "/specificArea/", true);
	}
	
	private void writeCountsAndOriginalCounts(Stations stations) {
		log.info("Do the converting ------------------");
		Converter converter = new Converter();
		converter.convert(stations.getCountStations());
		
		String outpath = configGroup.getOutputDir();

		CountsWriter writer = new CountsWriter(converter.getCountsIVTCH());
		writer.write(outpath + "/countsIVTCH.xml");
		writer = new CountsWriter(converter.getCountsTeleatlas());
		writer.write(outpath + "/countsTeleatlas.xml");
		writer = new CountsWriter(converter.getCountsNavteq());
		writer.write(outpath + "/countsNAVTEQ.xml");
		
		// ---------------------------------------------------------------------------------------
		//log.info("Writing old files  -------------------");
		//for comparison with old files
		//Counts countsTele = new Counts();
		//new File(outpath + "/original/").mkdirs();
		//MatsimCountsReader countsReaderTele = new MatsimCountsReader(countsTele);
		//countsReaderTele.readFile(inpath + "/original/countsTeleatlas.xml");
		//CountsWriter countsWriterTele = new CountsWriter(countsTele);
		//countsWriterTele.write(outpath + "/original/countsTeleatlas_original.xml");
		//
		//Counts countsIVTCH = new Counts();
		//MatsimCountsReader countsReaderIVTCH = new MatsimCountsReader(countsIVTCH);
		//countsReaderIVTCH.readFile(inpath + "/original/countsIVTCH.xml");
		//CountsWriter countsWriterIVTCH = new CountsWriter(countsIVTCH);
		//countsWriterIVTCH.write(outpath + "/original/countsIVTCH_original.xml");	
		//		
		//Counts countsNAVTEQ = new Counts();
		//MatsimCountsReader countsReaderNAVTEQ = new MatsimCountsReader(countsNAVTEQ);
		//countsReaderNAVTEQ.readFile(inpath + "/original/countsNAVTEQ.xml");
		//CountsWriter countsWriterNAVTEQ = new CountsWriter(countsNAVTEQ);
		//countsWriterNAVTEQ.write(outpath + "/original/countsNavteq_original.xml");
	}
}

