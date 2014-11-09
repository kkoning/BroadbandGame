package net.kkoning.ace.broadband;

import java.util.ArrayList;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Test;

import ec.Individual;
import ec.agency.eval.EvaluationGroup;

public class BroadbandModelTests {

	@Test
	public void testStep() {
		BroadbandModel m = initModel();
		for (int i = 0; i < 10; i++) {
			m.step();
		}
		
		System.out.println("NSP Surplusses");
		for (NSPAgent nsp : m.nsps) {
			System.out.println(nsp.getMoney());
		}
		
		SummaryStatistics ss = new SummaryStatistics();
		for (int i = 0; i < m.numConsumerAgents; i++) {
			ss.addValue(m.consumerSurplus[i]);
		}
		System.out.println("For Consumer Surplus, " + ss);
		
	}

	public BroadbandModel initModel() {

		// Use a pair of TestNSPIndividuals
		EvaluationGroup eg = new EvaluationGroup();
		eg.individuals = new ArrayList<Individual>();
		for (int i = 0; i < 2; i++)
			eg.individuals.add(new TestNSPIndividual());

		BroadbandModel m = new BroadbandModel();
		m.setGeneration(1);
		m.setSimulationID(1);
		m.setSeed(1);
		m.setEvaluationGroup(eg);
		
		m.numConsumerAgents = 1000;
		m.maxConsumerWTP = 100f;
		m.perConsumerWTPReduction = 0.1f;
		m.initalizeConsumers();
		
		m.s_alpha = 20f;
		m.s_beta = 5f;
				
		
		m.checkReadyToStart();
		

		return m;
	}

}
