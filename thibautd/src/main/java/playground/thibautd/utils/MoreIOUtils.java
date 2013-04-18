/* *********************************************************************** *
 * project: org.matsim.*
 * MoreIOUtils.java
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
package playground.thibautd.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * Defines some useful i/o related methods, which are not part of the
 * core MATSim IOUtils class.
 * @author thibautd
 */
public class MoreIOUtils {
	private static final String LOGFILE = "MyLogFile.log";
	private static final String WARNLOGFILE = "MyWarnLogFile.log";

	private MoreIOUtils() {
		//no instanciation 
	}

	/**
	 * creates an output directory if it does not exists, and creates a logfile.
	 */
	public static void initOut( String outputDir ) {
		try {
			// create directory if does not exist
			if (!outputDir.endsWith("/")) {
				outputDir += "/";
			}
			File outputDirFile = new File(outputDir);
			if (!outputDirFile.exists()) {
				outputDirFile.mkdirs();
			}

			// init logFile
			CollectLogMessagesAppender appender = new CollectLogMessagesAppender();
			Logger.getRootLogger().addAppender(appender);

			initOutputDirLogging(
				outputDir,
				appender.getLogEvents());
		} catch (IOException e) {
			// do NOT continue without proper logging!
			throw new RuntimeException("error while creating log file",e);
		}
	}

	/**
	 * Redefine the IOUtils method, with a different file and appender name.
	 * Otherwise, runing a controler in a borader context stops the logging at
	 * the end of the controler shutdown.
	 */
	public static void initOutputDirLogging(
			final String outputDirectory,
			final List<LoggingEvent> logEvents) throws IOException {
		Logger root = Logger.getRootLogger();
		FileAppender appender = new FileAppender(Controler.DEFAULTLOG4JLAYOUT, outputDirectory +
				System.getProperty("file.separator") + LOGFILE);
		appender.setName(LOGFILE);
		root.addAppender(appender);
		FileAppender warnErrorAppender = new FileAppender(Controler.DEFAULTLOG4JLAYOUT, outputDirectory +
				System.getProperty("file.separator") +  WARNLOGFILE);
		warnErrorAppender.setName(WARNLOGFILE);
		warnErrorAppender.setThreshold(Level.WARN);
//		LevelRangeFilter filter = new LevelRangeFilter();
//		filter.setLevelMax(Level.ALL);
//		filter.setAcceptOnMatch(true);
//		filter.setLevelMin(Level.WARN);
//		warnErrorAppender.addFilter(filter);
		root.addAppender(warnErrorAppender);
		if (logEvents != null) {
			for (LoggingEvent e : logEvents) {
				appender.append(e);
				if (e.getLevel().isGreaterOrEqual(Level.WARN)) {
					warnErrorAppender.append(e);
				}
			}
		}
	}

	/**
	 * Redefine the IOUtils method, with a different file and appender name.
	 * Otherwise, runing a controler in a borader context stops the logging at
	 * the end of the controler shutdown.
	 */
	public static void closeOutputDirLogging() {
		Logger root = Logger.getRootLogger();
		Appender app = root.getAppender(LOGFILE);
		root.removeAppender(app);
		app.close();
		app = root.getAppender(WARNLOGFILE);
		root.removeAppender(app);
		app.close();
	}

	public static void writeLines(
			final BufferedWriter writer,
			final String... lines) {
		try {
			for ( String l : lines ) {
				writer.write( l );
				writer.newLine();
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}
}

