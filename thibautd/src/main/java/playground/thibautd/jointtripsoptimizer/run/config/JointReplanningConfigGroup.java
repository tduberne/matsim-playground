/* *********************************************************************** *
 * project: org.matsim.*
 * JointReplanningConfigGroup.java
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
package playground.thibautd.jointtripsoptimizer.run.config;

import java.lang.reflect.Field;
import java.lang.String;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup.TripStructureAnalysisLayerOption;
import org.matsim.core.config.groups.PlanomatConfigGroup.SimLegInterpretation;
import org.matsim.core.config.Module;

/**
 * Gives access to all parameters needed for the joint plan optimisation.
 * @author thibautd
 */
public class JointReplanningConfigGroup extends Module {

	private static final Logger log = Logger.getLogger(JointReplanningConfigGroup.class);

	private static final long serialVersionUID = 1L;
	public static final String GROUP_NAME = "JointReplanning";

	//parameter names
	/**
	 * the size of the genetic population.
	 */
	public static final String POP_SIZE = "gaPopulationSize";
	/**
	 * probability with wich the mutation operator is aplied on a gene.
	 */
	public static final String MUTATION_PROB = "mutationProbability";
	/**
	 * rate of the whole arithmetical CO: rate * populationSize couples will be
	 * mated.
	 */
	public static final String WHOLE_CO_PROB = "WholeArithmeticalCrossOverRate";
	/**
	 * rate of the simple arithmetical CO: rate * populationSize couples will be
	 * mated.
	 */
	public static final String SIMPLE_CO_PROB = "SimpleArithmeticalCrossOverRate";
	/**
	 * rate of the single arithmetical CO: rate * populationSize couples will be
	 * mated.
	 */
	public static final String SINGLE_CO_PROB = "SingleArithmeticalCrossOverRate";
	/**
	 * non-unifority parameter: the higher it is, the quicker the mutation of
	 * double values becomes small.
	 */
	public static final String NON_UNIFORMITY_PARAM = "mutationNonUniformity";
	/**
	 * determines if the joint trip participation is to optimize.
	 */
	public static final String OPTIMIZE_TOGGLE = "toggleToOptimize";
	/**
	 * For analysis purpose: output plot for each optimised plan. To use
	 * on little scenarios only!
	 */
	public static final String PLOT_FITNESS = "plotFitnessEvolution";
	/**
	 * Determines if the mode is optimized. This should always be set to "true"
	 * if the participation is optimized.
	 */
	public static final String OPTIMIZE_MODE = "modeToOptimize";
	/**
	 * The mode choice set.
	 */
	public static final String AVAIL_MODES = "availableModes";
	/**
	 * true if the stop condition is based on fitness evolution, false
	 * if the maximum number of iterations should always be performed.
	 */
	public static final String DO_MONITOR = "fitnessToMonitor";
	/**
	 * number of iterations before the fitness monitoring begins.
	 */
	public static final String ITER_MIN_NUM = "minNumberOfGAIterations";
	/**
	 * maximum number of generations.
	 */
	public static final String ITER_NUM = "maxNumberOfGAIterations";
	/**
	 * number of generations to wait between each best fitness measurement.
	 */
	public static final String MONITORING_PERIOD = "fitnessMonitoringPeriod";
	/**
	 * if the difference between two consecutive best fitness measurements are 
	 * below this value, the iterations are stopped.
	 */
	public static final String MIN_IMPROVEMENT = "minimumFitnessImprovementCHF";
	/**
	 * "layer" at which the subtour should be determined: facility or link.
	 * should always be set to link: pick up and drop off have their own facilities
	 */
	public static final String STRUCTURE_LAYER = "tripStructureAnalysisLayer";
	/**
	 * "Scale" of the discrete distance in the restricted tournament selection
	 */
	public static final String DISCRETE_DIST_SCALE = "discreteDistanceScale";
	/**
	 * "Cetin" or "CharyparEtAl". determines whether the first or the last link
	 * of a route is taken into acount when estimating travel time.
	 */
	public static final String SIM_LEG_INT = "simLegInterpretation";
	/**
	 * number of competitors in the tournaments
	 */
	public static final String RTS_WINDOW = "rtsWindowSize";
	/**
	 * true: the offsprings resulting from cross overs are mutated before selection
	 * false: at each iteration, new chromosomes are generated by mutating the old ones,
	 * and compete with them in selection.
	 */
	public static final String IN_PLACE = "mutationInPlace";
	/**
	 * Knowing that a double gene will be mutated, probability for the mutation
	 * to be non uniform.
	 */
	public static final String P_NON_UNIFORM = "probNonUniform";
	// deprecated parameters
	private static final String NUM_TIME_INTERVALS = "numTimeIntervals";
	private static final String DO_DUR = "dropOffDuration";
	private static final String SELECTION_THRESHOLD = "bestSelectionThreshold";
	private static final String DOUBLETTES = "geneticSelectionWithReplacement";
	private static final String DYNAMIC_CO_RATES = "allowAdaptiveCrossOverRates";
	private static final String SPX_RATE = "SPXCrossOverRate";
	private static final String SPX_SONS = "SPXOffspringRate";
	private static final String MAX_TABU_LENGTH = "tabuListMaxLength";

	//parameter values, initialized to defaults.

	private int populationSize = 70;
	private double mutationProb = 0.1;
	private double wholeCrossOverProb = 0.3;
	private double simpleCrossOverProb = 0.5;
	private double singleCrossOverProb = 0.3;
	private int numberOfIterations = 30;
	private double betaNonUniformity = 1;
	private boolean optimizeToggle = true;
	private boolean plotFitness = false;
	private boolean optimizeMode = true;
	private List<String> availableModes = null;
	private static final String defaultModes = "car,pt,walk,bike";
	private int minNumberOfIterations = 3;
	private int monitoringPeriod = 1;
	private boolean doMonitor = true;
	private double minImprovement = 0.075d;
	private TripStructureAnalysisLayerOption
		tripStructureAnalysisLayer = TripStructureAnalysisLayerOption.link;
	private double discreteDistScale = 17000d;
	private SimLegInterpretation simLegInt = SimLegInterpretation.CharyparEtAlCompatible;
	private int rtsWindowSize = 15;
	private boolean inPlaceMutation = true;
	private double pNonUniform = 0.5d;
	//deprecated fields:
	private int numTimeIntervals;
	private double dropOffDuration = 0;
	private double selectionThreshold = 0.1d;
	private boolean allowDoublettes = false;
	private boolean dynamicCoRate = true;
	private double spxRate = 0.0d;
	private int spxOffspringRate = 0;
	private int maxTabuLength = Integer.MAX_VALUE;

	public JointReplanningConfigGroup() {
		super(GROUP_NAME);
		log.debug("joint replanning config group initialized");
	}

	/*
	 * =========================================================================
	 * base class methods
	 * =========================================================================
	 */
	@Override
	public void addParam(String param_name, String value) {
		if (param_name.equals(NUM_TIME_INTERVALS)) {
			this.setNumTimeIntervals(value);
		}
		else if (param_name.equals(POP_SIZE)) {
			this.setPopulationSize(value);
		}
		else if (param_name.equals(DO_DUR)) {
			this.setDropOffDuration(value);
		}
		else if (param_name.equals(MUTATION_PROB)) {
			this.setMutationProbability(value);
		}
		else if (param_name.equals(WHOLE_CO_PROB)) {
			this.setWholeCrossOverProbability(value);
		}
		else if (param_name.equals(SIMPLE_CO_PROB)) {
			this.setSimpleCrossOverProbability(value);
		}
		else if (param_name.equals(SINGLE_CO_PROB)) {
			this.setSingleCrossOverProbability(value);
		}
		else if (param_name.equals(ITER_NUM)) {
			this.setMaxIterations(value);
		}
		else if (param_name.equals(NON_UNIFORMITY_PARAM)) {
			this.setMutationNonUniformity(value);
		}
		else if (param_name.equals(OPTIMIZE_TOGGLE)) {
			this.setOptimizeToggle(value);
		}
		else if (param_name.equals(SELECTION_THRESHOLD)) {
			this.setSelectionThreshold(value);
		}
		else if (param_name.equals(PLOT_FITNESS)) {
			this.setPlotFitness(value);
		}
		else if (param_name.equals(OPTIMIZE_MODE)) {
			this.setModeToOptimize(value);
		}
		else if (param_name.equals(AVAIL_MODES)) {
			this.setAvailableModes(value);
		}
		else if (param_name.equals(ITER_MIN_NUM)) {
			this.setMinIterations(value);
		}
		else if (param_name.equals(MONITORING_PERIOD)) {
			this.setMonitoringPeriod(value);
		}
		else if (param_name.equals(DO_MONITOR)) {
			this.setFitnessToMonitor(value);
		}
		else if (param_name.equals(MIN_IMPROVEMENT)) {
			this.setMinImprovement(value);
		}
		else if (param_name.equals(STRUCTURE_LAYER)) {
			this.setTripStructureAnalysisLayer(value);
		}
		else if (param_name.equals(DOUBLETTES)) {
			this.setAllowDoublettes(value);
		}
		else if (param_name.equals(DYNAMIC_CO_RATES)) {
			this.setIsDynamicCO(value);
		}
		else if (param_name.equals(SPX_RATE)) {
			this.setSPXProbability(value);
		}
		else if (param_name.equals(SPX_SONS)) {
			this.setSPXOffspringRate(value);
		}
		else if (param_name.equals(DISCRETE_DIST_SCALE)) {
			this.setDiscreteDistanceScale(value);
		}
		else if (param_name.equals(MAX_TABU_LENGTH)) {
			this.setTabuListMaxLength(value);
		}
		else if (param_name.equals(RTS_WINDOW)) {
			this.setRtsWindowSize(value);
		}
		else if (param_name.equals(SIM_LEG_INT)) {
			this.setSimLegInterpretation(value);
		}
		else if (param_name.equals(IN_PLACE)) {
			this.setInPlaceMutation(value);
		}
		else if (param_name.equals(P_NON_UNIFORM)) {
			this.setNonUniformMutationProbability(value);
		}
		else {
			log.warn("Unrecognized JointReplanning parameter: "+
					param_name+", of value: "+value+".");
		}
	}

	@Override
	public String getValue(String param_name) {
		if (param_name.equals(NUM_TIME_INTERVALS)) {
			return String.valueOf(this.getNumTimeIntervals());
		}
		else if (param_name.equals(POP_SIZE)) {
			return String.valueOf(this.getPopulationSize());
		}
		else if (param_name.equals(DO_DUR)) {
			return String.valueOf(this.getDropOffDuration());
		}
		else if (param_name.equals(MUTATION_PROB)) {
			return String.valueOf(this.getMutationProbability());
		}
		else if (param_name.equals(WHOLE_CO_PROB)) {
			return String.valueOf(this.getWholeCrossOverProbability());
		}
		else if (param_name.equals(SIMPLE_CO_PROB)) {
			return String.valueOf(this.getSimpleCrossOverProbability());
		}
		else if (param_name.equals(SINGLE_CO_PROB)) {
			return String.valueOf(this.getSingleCrossOverProbability());
		}
		else if (param_name.equals(ITER_NUM)) {
			return String.valueOf(this.getMaxIterations());
		}
		else if (param_name.equals(NON_UNIFORMITY_PARAM)) {
			return String.valueOf(this.getMutationNonUniformity());
		}
		else if (param_name.equals(OPTIMIZE_TOGGLE)) {
			return String.valueOf(this.getOptimizeToggle());
		}
		else if (param_name.equals(SELECTION_THRESHOLD)) {
			return String.valueOf(this.getSelectionThreshold());
		}
		else if (param_name.equals(PLOT_FITNESS)) {
			return String.valueOf(this.getPlotFitness());
		}
		else if (param_name.equals(OPTIMIZE_MODE)) {
			return String.valueOf(this.getModeToOptimize());
		}
		else if (param_name.equals(AVAIL_MODES)) {
			//TODO: do not produce an "inputable" value
			return String.valueOf(this.getAvailableModes());
		}
		else if (param_name.equals(ITER_MIN_NUM)) {
			return String.valueOf(this.getMinIterations());
		}
		else if (param_name.equals(MONITORING_PERIOD)) {
			return String.valueOf(this.getMonitoringPeriod());
		}
		else if (param_name.equals(DO_MONITOR)) {
			return String.valueOf(this.getFitnessToMonitor());
		}
		else if (param_name.equals(MIN_IMPROVEMENT)) {
			return String.valueOf(this.getMinImprovement());
		}
		else if (param_name.equals(STRUCTURE_LAYER)) {
			return this.getTripStructureAnalysisLayer().toString();
		}
		else if (param_name.equals(DOUBLETTES)) {
			return String.valueOf(this.getAllowDoublettes());
		}
		else if (param_name.equals(DYNAMIC_CO_RATES)) {
			return String.valueOf(this.getIsDynamicCO());
		}
		else if (param_name.equals(SPX_RATE)) {
			return String.valueOf(this.getSPXProbability());
		}
		else if (param_name.equals(SPX_SONS)) {
			return String.valueOf(this.getSPXOffspringRate());
		}
		else if (param_name.equals(DISCRETE_DIST_SCALE)) {
			return String.valueOf(this.getDiscreteDistanceScale());
		}
		else if (param_name.equals(MAX_TABU_LENGTH)) {
			return String.valueOf(this.getTabuListMaxLength());
		}
		else if (param_name.equals(RTS_WINDOW)) {
			return String.valueOf(this.getRtsWindowSize());
		}
		else if (param_name.equals(SIM_LEG_INT)) {
			return String.valueOf(this.getSimLegInterpretation());
		}
		else if (param_name.equals(IN_PLACE)) {
			return String.valueOf(this.getInPlaceMutation());
		}
		else if (param_name.equals(P_NON_UNIFORM)) {
			return String.valueOf(this.getNonUniformMutationProbability());
		}

		return null;
	}

	@Override
	public TreeMap<String,String> getParams() {
		TreeMap<String,String> map = new TreeMap<String,String>();
		this.addParameterToMap(map, NUM_TIME_INTERVALS);
		this.addParameterToMap(map, POP_SIZE);
		this.addParameterToMap(map, DO_DUR);
		this.addParameterToMap(map, MUTATION_PROB);
		this.addParameterToMap(map, WHOLE_CO_PROB);
		this.addParameterToMap(map, SIMPLE_CO_PROB);
		this.addParameterToMap(map, SINGLE_CO_PROB);
		this.addParameterToMap(map, ITER_NUM);
		this.addParameterToMap(map, NON_UNIFORMITY_PARAM);
		this.addParameterToMap(map, OPTIMIZE_TOGGLE);
		this.addParameterToMap(map, SELECTION_THRESHOLD);
		this.addParameterToMap(map, PLOT_FITNESS);
		this.addParameterToMap(map, OPTIMIZE_MODE);
		this.addParameterToMap(map, AVAIL_MODES);
		this.addParameterToMap(map, ITER_MIN_NUM);
		this.addParameterToMap(map, MONITORING_PERIOD);
		this.addParameterToMap(map, DO_MONITOR);
		this.addParameterToMap(map, STRUCTURE_LAYER);
		this.addParameterToMap(map, DOUBLETTES);
		this.addParameterToMap(map, DYNAMIC_CO_RATES);
		this.addParameterToMap(map, SPX_RATE);
		this.addParameterToMap(map, SPX_SONS);
		this.addParameterToMap(map, DISCRETE_DIST_SCALE);
		this.addParameterToMap(map, RTS_WINDOW);
		this.addParameterToMap(map, SIM_LEG_INT);
		this.addParameterToMap(map, IN_PLACE);
		this.addParameterToMap(map, P_NON_UNIFORM);
		return map;
	}

	/*
	 * =========================================================================
	 * getters/setters
	 * =========================================================================
	 */

	public int getNumTimeIntervals() {
		return this.numTimeIntervals;
	}

	public void setNumTimeIntervals(String numTimeIntervals) {
		this.numTimeIntervals = Integer.parseInt(numTimeIntervals);
	}

	public int getPopulationSize() {
		return this.populationSize;
	}

	public void setPopulationSize(String populationSize) {
		this.populationSize = Integer.parseInt(populationSize);
	}

	public double getDropOffDuration() {
		return this.dropOffDuration;
	}
	
	public void setDropOffDuration(String dropOffDuration) {
		this.dropOffDuration = Double.valueOf(dropOffDuration);
	}

	public double getMutationProbability() {
		return this.mutationProb;
	}

	public void setMutationProbability(String mutationProb) {
		this.mutationProb = Double.valueOf(mutationProb);

		if ((this.mutationProb < 0)||(this.mutationProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public double getWholeCrossOverProbability() {
		return this.wholeCrossOverProb;
	}

	public void setWholeCrossOverProbability(String coProb) {
		this.wholeCrossOverProb = Double.valueOf(coProb);

		if ((this.wholeCrossOverProb < 0)||(this.wholeCrossOverProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public double getSingleCrossOverProbability() {
		return this.singleCrossOverProb;
	}

	public void setSingleCrossOverProbability(String coProb) {
		this.singleCrossOverProb = Double.valueOf(coProb);

		if ((this.singleCrossOverProb < 0)||(this.singleCrossOverProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public double getSimpleCrossOverProbability() {
		return this.simpleCrossOverProb;
	}

	public void setSimpleCrossOverProbability(String coProb) {
		this.simpleCrossOverProb = Double.valueOf(coProb);

		if ((this.simpleCrossOverProb < 0)||(this.simpleCrossOverProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public int getMaxIterations() {
		return this.numberOfIterations;
	}

	public void setMaxIterations(String iterations) {
		this.numberOfIterations = Integer.parseInt(iterations);

		if (this.numberOfIterations < 0) {
			throw new IllegalArgumentException("number of iterations must be positive");
		}
	}

	public double getMutationNonUniformity() {
		return this.betaNonUniformity;
	}

	public void setMutationNonUniformity(String beta) {
		this.betaNonUniformity = Double.valueOf(beta);

		if (this.betaNonUniformity <= 0d) {
			throw new IllegalArgumentException("non uniformity mutation parameter"+
					" must be positive");
		}
	}

	public boolean getOptimizeToggle() {
		return this.optimizeToggle;
	}

	public void setOptimizeToggle(String value) {
		if (value.toLowerCase().equals("true")) {
			this.optimizeToggle = true;
		}
		else if (value.toLowerCase().equals("false")) {
			this.optimizeToggle = false;
		}
		else {
			throw new IllegalArgumentException("value for "+
					OPTIMIZE_TOGGLE+" must be \"true\" or \"false\"");
		}
	}

	public double getSelectionThreshold() {
		return this.selectionThreshold;
	}

	public void setSelectionThreshold(String value) {
		this.selectionThreshold = Double.parseDouble(value);

		if ((this.selectionThreshold < 0d)||(this.selectionThreshold > 1d)) {
			throw new IllegalArgumentException("the selection threshold must belong"
					+" to [0,1]!");
		}
	}

	public boolean getPlotFitness() {
		return this.plotFitness;
	}

	public void setPlotFitness(String value) {
		if (value.toLowerCase().equals("true")) {
			this.plotFitness = true;
		}
		else if (value.toLowerCase().equals("false")) {
			this.plotFitness = false;
		}
		else {
			throw new IllegalArgumentException("value for "+
					OPTIMIZE_TOGGLE+" must be \"true\" or \"false\"");
		}
	}

	public List<String> getAvailableModes() {
		if (this.availableModes == null) {
			//log.warn("modes available for the optimisation initialized to the "+
			//		"set of all available values");
			//this.availableModes = getAllModes();
			log.info("using default mode choice set: "+defaultModes);
			this.setAvailableModes(defaultModes);
		}
		return this.availableModes;
	}

	public void setAvailableModes(String value) {
		String[] modes = value.split(",");

		//List<String> allModes = getAllModes();
		this.availableModes = new ArrayList<String>();

		for (String mode : modes) {
		//	if (allModes.contains(mode)) {
			try {
				this.availableModes.add((String)
						TransportMode.class.getField(mode).get(null));
			}
			//else {
			catch (NoSuchFieldException e) {
				throw new IllegalArgumentException("unrecognized mode: "+mode);
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("error while iterating over "+
						"TransportMode fields");
			}
		}
	}

	private static List<String> getAllModes() {
		List<String> out = new ArrayList<String>();

		//iterate over all public fields of transport mode
		//TODO: more precise catches.
		for (Field field : TransportMode.class.getFields()) {
			try {
				out.add((String) field.get(null));
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("error while iterating over "+
						"TransportMode fields");
			}
		}

		return out;
	}

	public void setModeToOptimize(String value) {
		this.optimizeMode = Boolean.valueOf(value);
	}

	public boolean getModeToOptimize() {
		return this.optimizeMode;
	}

	public void setFitnessToMonitor(String value) {
		this.doMonitor = Boolean.parseBoolean(value);
	}

	public boolean getFitnessToMonitor() {
		return this.doMonitor;
	}

	public void setMinIterations(String value) {
		this.minNumberOfIterations = Integer.valueOf(value);
	}

	public int getMinIterations() {
		return this.minNumberOfIterations;
	}

	public void setMonitoringPeriod(String value) {
		this.monitoringPeriod = Integer.valueOf(value);
	}

	public int getMonitoringPeriod() {
		return this.monitoringPeriod;
	}

	public void setMinImprovement(String value) {
		this.minImprovement = Double.parseDouble(value);
	}

	public double getMinImprovement() {
		return this.minImprovement;
	}

	public void setTripStructureAnalysisLayer(String value) {
		if (value.equals("facility")) {
			this.tripStructureAnalysisLayer =
				PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility;
		}
		else if (value.equals("link")) {
			this.tripStructureAnalysisLayer =
				PlanomatConfigGroup.TripStructureAnalysisLayerOption.link;
		}
		else {
			throw new IllegalArgumentException("the tripStructureAnalysisLayer option"+
					" must be facility or link.");
		}
	}

	public TripStructureAnalysisLayerOption getTripStructureAnalysisLayer() {
			return this.tripStructureAnalysisLayer;
	}

	public boolean getAllowDoublettes() {
		return this.allowDoublettes;
	}

	public void setAllowDoublettes(String value) {
		this.allowDoublettes = Boolean.parseBoolean(value);
	}

	public boolean getIsDynamicCO() {
		return this.dynamicCoRate;
	}

	public void setIsDynamicCO(String value) {
		this.dynamicCoRate = Boolean.parseBoolean(value);
	}

	public double getSPXProbability() {
		return this.spxRate;
	}

	public void setSPXProbability(String coProb) {
		this.spxRate = Double.valueOf(coProb);

		if ((this.spxRate < 0)||(this.spxRate > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public void setSPXOffspringRate(String value) {
		this.spxOffspringRate = Integer.valueOf(value);
		if (this.spxOffspringRate < 0) {
			throw new IllegalArgumentException("offspring rate must be positive");
		}
	}

	public int getSPXOffspringRate() {
		return this.spxOffspringRate;
	}

	public void setDiscreteDistanceScale(String value) {
		this.discreteDistScale = Double.valueOf(value);
	}

	public double getDiscreteDistanceScale() {
		return this.discreteDistScale;
	}

	public void setTabuListMaxLength(String value) {
		this.maxTabuLength = Math.max(0, Integer.valueOf(value));
	}

	public int getTabuListMaxLength() {
		return this.maxTabuLength;
	}

	public void setSimLegInterpretation(String value) {
		this.simLegInt = SimLegInterpretation.valueOf(value);
	}

	public SimLegInterpretation getSimLegInterpretation() {
		return this.simLegInt;
	}

	public void setRtsWindowSize(String value) {
		this.rtsWindowSize = Integer.valueOf(value);
	}

	public int getRtsWindowSize() {
		return this.rtsWindowSize;
	}

	public void setInPlaceMutation(final String value) {
		this.inPlaceMutation = Boolean.valueOf(value);
	}

	public boolean getInPlaceMutation() {
		return this.inPlaceMutation;
	}

	public void setNonUniformMutationProbability(final String value) {
		this.pNonUniform = Double.parseDouble(value);
	}

	public double getNonUniformMutationProbability() {
		return this.pNonUniform;
	}

	// allow setting of GA params "directly"
	public void setPopulationSize(final int populationSize) {
		this.populationSize = (populationSize);
	}

	public void setMutationProbability(final double mutationProb) {
		this.mutationProb = (mutationProb);

		if ((this.mutationProb < 0)||(this.mutationProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public void setWholeCrossOverProbability(final double coProb) {
		this.wholeCrossOverProb = (coProb);

		if ((this.wholeCrossOverProb < 0)||(this.wholeCrossOverProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public void setSingleCrossOverProbability(final double coProb) {
		this.singleCrossOverProb = (coProb);

		if ((this.singleCrossOverProb < 0)||(this.singleCrossOverProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public void setSimpleCrossOverProbability(final double coProb) {
		this.simpleCrossOverProb = (coProb);

		if ((this.simpleCrossOverProb < 0)||(this.simpleCrossOverProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public void setMaxIterations(final int iterations) {
		this.numberOfIterations = (iterations);

		if (this.numberOfIterations < 0) {
			throw new IllegalArgumentException("number of iterations must be positive");
		}
	}

	public void setMutationNonUniformity(final double beta) {
		this.betaNonUniformity = (beta);

		if (this.betaNonUniformity <= 0d) {
			throw new IllegalArgumentException("non uniformity mutation parameter"+
					" must be positive");
		}
	}

	public void setMinIterations(final int value) {
		this.minNumberOfIterations = (value);
	}

	public void setMonitoringPeriod(final int  value) {
		this.monitoringPeriod = value;
	}

	public void setMinImprovement(final double value) {
		this.minImprovement = value;
	}

	public void setDiscreteDistanceScale(final double value) {
		this.discreteDistScale = value;
	}

	public void setRtsWindowSize(final int value) {
		this.rtsWindowSize = value;
	}

	public void setInPlaceMutation(final boolean value) {
		this.inPlaceMutation = (value);
	}

	public void setNonUniformMutationProbability(final double value) {
		this.pNonUniform = (value);
	}
}

