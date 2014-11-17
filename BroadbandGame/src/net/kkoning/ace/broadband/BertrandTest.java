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

public class BertrandTest implements Runnable {
	
	AgencyEvolutionState evoState;
	File configFile;
	DataOutputFile out;
	AgencyRunner runner;
	
	public static void main(String[] args) throws Exception {
		BertrandTest test = new BertrandTest();
//		test.configFile = new File((String) args[0]);
		test.configFile = new File("/Core/home/kkoning/research/ace/broadband/BertrandTest/net.kkoning.ace.broadband-evolve.properties");
		
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
			
			String[] headers = new String[10];
			headers[0] = "modelID";
			headers[1] = "Step";
			headers[2] = "A_Price";
			headers[3] = "A_Capacity";
			headers[4] = "A_Customers";
			headers[5] = "A_Fitness";
			headers[6] = "B_Price";
			headers[7] = "B_Capacity";
			headers[8] = "B_Customers";
			headers[9] = "B_Fitness";
			out = new DataOutputFile("bertrand.tsv", headers);
			
			runner = evoState.getRunner();
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		for (int i = 0; i < 10; i++) {
			BroadbandModel model = getBroadbandModel();
			model.setSimulationID(i);
			ModelHelper helper = new ModelHelper();
			helper.model = model;
			helper.test = this;
			
			helper.run();
			
		}
		
		out.close();
	}
	
	public BroadbandModel getBroadbandModel() {
		BroadbandModel model = new BroadbandModel();
		model.setup(evoState, new Parameter("eval").push("model"));
		model.modelID = 0;
		model.generation = 0;
		model.setSeed(evoState.random[0].nextInt());
		model.initalizeConsumers();
		
		return model;
	}

	class ModelHelper implements Runnable {

		BertrandTest test;
		BroadbandModel model;
		
		@Override
		public void run() {
			EvaluationGroup eg = new EvaluationGroup();
			eg.individuals = new ArrayList<Individual>();
			eg.individuals.add(new BertrandNSPIndividual());
			eg.individuals.add(new BertrandNSPIndividual());
			
			model.setEvaluationGroup(eg);
			
			
			while (model.step < model.steps) {
				Object[] toWrite = new Object[10];
				toWrite[0] = model.getSimulationID();
				toWrite[1] = model.step;
				toWrite[2] = model.nsps[0].price;
				toWrite[3] = model.nsps[0].networkCapacity;
				toWrite[4] = model.nsps[0].numCustomers;
				toWrite[5] = model.nsps[0].money;
				toWrite[6] = model.nsps[1].price;
				toWrite[7] = model.nsps[1].networkCapacity;
				toWrite[8] = model.nsps[1].numCustomers;
				toWrite[9] = model.nsps[1].money;
				out.writeTuple(toWrite);
				model.step();
			}

			
		}
		
	}
	
	



	
}
