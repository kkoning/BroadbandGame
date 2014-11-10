package net.kkoning.ace.broadband;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import ec.Fitness;
import ec.Individual;
import ec.agency.AgencyEvolutionState;
import ec.agency.eval.AgencyRunner;
import ec.agency.eval.EvaluationGroup;
import ec.agency.io.DataOutputFile;
import ec.simple.SimpleFitness;
import ec.util.MersenneTwisterFast;
import ec.util.Output;
import ec.util.Parameter;
import ec.util.ParameterDatabase;

public class SmartGradientTest implements Runnable {
	
	AgencyEvolutionState evoState;
	File configFile;
	DataOutputFile out;
	AgencyRunner runner;
	
	public static void main(String[] args) throws Exception {
		SmartGradientTest test = new SmartGradientTest();
		test.configFile = new File((String) args[0]);
//		test.configFile = new File("net.kkoning.ace.broadband-evolve.properties");
		test.run();
	}
	
	@Override
	public void run() {
		// Try to set up a model factory.
		try {
			ParameterDatabase pd = new ParameterDatabase(configFile);
			
			evoState = new AgencyEvolutionState();
			evoState.parameters = pd;
			evoState.output = new Output(false, 0);
			evoState.job = new Object[1];
			evoState.job[0] = 9999;
			evoState.setup(evoState, null);
			evoState.random = new MersenneTwisterFast[1];
			evoState.random[0] = new MersenneTwisterFast(5);
			
			BroadbandModel model = getBroadbandModel();
			System.out.println("Model Creation successful " + model);
			
			String[] headers = new String[6];
			headers[0] = "A_Price";
			headers[1] = "A_Capacity";
			headers[2] = "A_Fitness";
			headers[3] = "B_Price";
			headers[4] = "B_Capacity";
			headers[5] = "B_Fitness";
			out = new DataOutputFile("smart_gradient.tsv", headers);
			
			runner = evoState.getRunner();
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		for (int i = 0; i < 1000000; i++) {
			BroadbandModel model = getBroadbandModel();
			ModelHelper helper = new ModelHelper();
			helper.model = model;
			helper.test = this;
			runner.runModel(helper);
		}
		
		
	}
	
	public BroadbandModel getBroadbandModel() {
		BroadbandModel model = new BroadbandModel();
		model.setup(evoState, new Parameter("eval").push("model"));
		model.modelID = 0;
		model.generation = 0;
		model.setSeed(evoState.random[0].nextInt());
		
		return model;
	}

	FloatVectorNSPIndividual getRandomInd() {
		float capacityMin = 0;
		float capacityRange = 100;
		float priceMin = 0;
		float priceRange = 100;
		
		float capacity = evoState.random[0].nextFloat() * capacityRange + capacityMin;
		float price = evoState.random[0].nextFloat() * priceRange + priceMin;
		
		float[] genome = new float[2];
		genome[0] = price;
		genome[1] = capacity;

		FloatVectorNSPIndividual ind = new SmartFloatVectorNSPIndividual();
		ind.genome = genome;
		return ind;
	}
	
	class ModelHelper implements Runnable {

		SmartGradientTest test;
		BroadbandModel model;
		
		@Override
		public void run() {
			EvaluationGroup eg = new EvaluationGroup();
			eg.individuals = new ArrayList<Individual>();
			eg.individuals.add(test.getRandomInd());
			eg.individuals.add(test.getRandomInd());
			
			model.setEvaluationGroup(eg);
			
			model.run();
			
			Map<Individual,Fitness> fit = model.getFitnesses();
			
			ArrayList<Object> toWrite = new ArrayList<Object>();
			for (Individual ind : fit.keySet()) {
				FloatVectorNSPIndividual nsp = (FloatVectorNSPIndividual) ind;
				toWrite.add(nsp.genome[0]); // price
				toWrite.add(nsp.genome[1]); // capacity
				SimpleFitness sf = (SimpleFitness) fit.get(nsp);
				toWrite.add(sf.fitness());
			}
			
			out.writeTuple(toWrite.toArray());
		}
		
	}
	
	



	
}
