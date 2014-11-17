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

public class CournotExpectationsGradientTest implements Runnable {
	
	AgencyEvolutionState evoState;
	File configFile;
	DataOutputFile out;
	AgencyRunner runner;
	
	public static void main(String[] args) throws Exception {
		CournotExpectationsGradientTest test = new CournotExpectationsGradientTest();
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
			
			String[] headers = new String[4];
			headers[0] = "A_Capacity";
			headers[1] = "A_Fitness";
			headers[2] = "B_Capacity";
			headers[3] = "B_Fitness";
			out = new DataOutputFile("cournot_gradient.tsv", headers);
			
			runner = evoState.getRunner();
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		for (int i = 0; i < 100000000; i++) {
			BroadbandModel model = getBroadbandModel();
			ModelHelper helper = new ModelHelper();
			helper.model = model;
			helper.test = this;
			runner.runModel(helper);
			if (i % 10000 == 0)
				System.out.println("Queued " + i + " models");
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

	FloatVectorNSPIndividual getFixedInd(float price, float capacity) {
		float[] genome = new float[2];
		genome[0] = price;
		genome[1] = capacity;

		FloatVectorNSPIndividual ind = new FloatVectorNSPIndividual();
		ind.genome = genome;
		return ind;
		
	}
	
	CournotExpectationsNSPIndividual getRandomInd() {
		float capacityMin = 0;
		float capacityRange = 50;
		
		float capacity = evoState.random[0].nextFloat() * capacityRange + capacityMin;
		
		float[] genome = new float[1];
		genome[0] = capacity;

		CournotExpectationsNSPIndividual ind = new CournotExpectationsNSPIndividual();
		ind.genome = genome;
		return ind;
	}
	
	class ModelHelper implements Runnable {

		CournotExpectationsGradientTest test;
		BroadbandModel model;
		
		@Override
		public void run() {
			EvaluationGroup eg = new EvaluationGroup();
			eg.individuals = new ArrayList<Individual>();
			CournotExpectationsNSPIndividual a = test.getRandomInd();
			CournotExpectationsNSPIndividual b = test.getRandomInd();
//			CournotExpectationsNSPIndividual b = test.getFixedInd(15f, 43f);
			
			eg.individuals.add(a);
			eg.individuals.add(b);
//			eg.individuals.add(test.getRandomInd());
			
			model.setEvaluationGroup(eg);
			
			model.run();
			
			Map<Individual,Fitness> fit = model.getFitnesses();
			
			ArrayList<Object> toWrite = new ArrayList<Object>();
			toWrite.add(a.genome[0]); // capacity
			SimpleFitness sf = (SimpleFitness) fit.get(a);
			toWrite.add(sf.fitness());
			toWrite.add(b.genome[0]); // capacity
			sf = (SimpleFitness) fit.get(b);
			toWrite.add(sf.fitness());
			
			out.writeTuple(toWrite.toArray());
		}
		
	}
	
	



	
}
