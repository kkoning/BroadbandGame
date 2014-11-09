package net.kkoning.ace.broadband;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.agency.AgencyEvolutionState;
import ec.agency.eval.AgencyModel;
import ec.agency.eval.EvaluationGroup;
import ec.agency.io.AgencyOutputFile;
import ec.simple.SimpleFitness;
import ec.util.MersenneTwister;
import ec.util.Parameter;

public class BroadbandModel implements AgencyModel {
	private static final long serialVersionUID = 1L;

	AgencyEvolutionState evoState;
	AgencyOutputFile out;
	Integer modelID;
	Integer generation;
	Integer randomSeed;
	MersenneTwister random;
	EvaluationGroup evalGroup;

	Integer numConsumerAgents;
	Float maxConsumerWTP;
	Float perConsumerWTPReduction;
	Float capacityCost;

	Float s_alpha;
	Float s_beta;
	double congest_penalty_exp = 1;
	Float utilityErrorRatio;

	NSPAgent[] nsps;

	float[] consumerWTP;
	byte[] consumerProviders;
	double[] consumerSurplus;

	// Simulation-wide averages
	Mean averagePriceOffer = new Mean();
	Mean averagePriceRange = new Mean();

	Mean averageQuantity = new Mean();
	Mean averageQuantityRange = new Mean();

	int step;
	int steps;

	void initalizeConsumers() {
		consumerWTP = new float[numConsumerAgents];
		consumerSurplus = new double[numConsumerAgents];
		consumerProviders = new byte[numConsumerAgents];
		for (int i = 0; i < numConsumerAgents; i++) {
			consumerWTP[i] = maxConsumerWTP - (i * perConsumerWTPReduction);
			consumerProviders[i] = -1; // start with no NSP
		}
	}

	public void perStepStats() {
		float highPrice = Float.MIN_VALUE, lowPrice = Float.MAX_VALUE;
		float highQty = Float.MIN_VALUE, lowQty = Float.MAX_VALUE;

		for (NSPAgent nsp : nsps) {
			averagePriceOffer.increment(nsp.price);
			averageQuantity.increment(nsp.networkCapacity);

			if (nsp.price > highPrice)
				highPrice = nsp.price;
			if (nsp.price < lowPrice)
				lowPrice = nsp.price;

			if (nsp.networkCapacity > highQty)
				highQty = nsp.networkCapacity;
			if (nsp.networkCapacity < lowQty)
				lowQty = nsp.networkCapacity;
		}

		averagePriceRange.increment(highPrice - lowPrice);
		averageQuantityRange.increment(highQty - lowQty);

	}

	@Override
	public void setup(EvolutionState state, Parameter base) {
		this.evoState = (AgencyEvolutionState) state;

		steps = state.parameters.getInt(base.push("steps"), null);

		numConsumerAgents = state.parameters.getInt(
				base.push("numConsumerAgents"), null);
		maxConsumerWTP = state.parameters.getFloat(base.push("maxConsumerWTP"),
				null);
		perConsumerWTPReduction = state.parameters.getFloat(
				base.push("perConsumerWTPReduction"), null);

		capacityCost = state.parameters.getFloat(base.push("capacityCost"),
				null);

		s_alpha = state.parameters.getFloat(base.push("s_alpha"), null);
		s_beta = state.parameters.getFloat(base.push("s_beta"), null);

		congest_penalty_exp = (double) state.parameters.getFloat(
				base.push("congest_penalty_exp"), null);

		utilityErrorRatio = state.parameters.getFloat(
				base.push("utilityErrorRatio"), null);

		out = evoState.getOutputFile(0);

	}

	@Override
	public void run() {
		initalizeConsumers();
		checkReadyToStart();

		for (step = 0; step < steps; step++) {
			step();
			perStepStats();
		}

		Object[] data = new Object[6];
		data[0] = cumulativeConsumerSurplus();
		data[1] = cumulativeProducerSurplus();
		data[2] = averagePriceOffer.getResult();
		data[3] = averagePriceRange.getResult();
		data[4] = averageQuantity.getResult();
		data[5] = averageQuantityRange.getResult();

		out.writeTuple(data);
	}

	void step() {
		for (NSPAgent nsp : nsps) {
			// NSPs set prices and capacity
			nsp.decidePrice();
			nsp.decideCapacity();

			// Bill NSPs for the capacity they've chosen
			nsp.capacityBill();
		}

		// Consumers make their purchasing decisions
		// consumerDecisions_RationalMomentum();
		consumerDecisions_Noisy();

		// NSPs collect on bills
		for (NSPAgent nsp : nsps)
			nsp.bill();

		step++;
	}

	void consumerDecisions_Noisy() {
		// Need to update # of consumers on each provider
		for (int i = 0; i < nsps.length; i++)
			nsps[i].numCustomers = 0; // reset

		// Each consumer
		for (int consumer_i = 0; consumer_i < numConsumerAgents; consumer_i++) {
			float u[] = new float[nsps.length];
			byte nsp_max_u_i = -1;
			float nsp_max_u = -1f;

			// Determine utilities for each NSP and which is best.
			List<Byte> bestNSPs = new ArrayList<Byte>();
			for (byte nsp_i = 0; nsp_i < nsps.length; nsp_i++) {
				u[nsp_i] = consumerSurplus(nsp_i, consumer_i);
				float error = random.nextFloat() * utilityErrorRatio;
				error = error - (utilityErrorRatio / 2);
				u[nsp_i] = u[nsp_i] + error;

				// If this NSP is better, clear the list of best NSPs and start
				// over with this one.  On the other hand, if this one is exactly
				// the same, just add it to the list of equally "best" NSPs.
				if (u[nsp_i] > nsp_max_u) {
					bestNSPs.clear();
					bestNSPs.add(nsp_i);
					nsp_max_u_i = nsp_i;
					nsp_max_u = u[nsp_i];
				} else if (u[nsp_i] == nsp_max_u) {
					bestNSPs.add(nsp_i);
				}
			}

			// If > 1 is best, choose each with equal probability
			if (bestNSPs.size() == 0) {
				nsp_max_u_i = -1;
			} else {
				nsp_max_u_i = bestNSPs.get(random.nextInt(bestNSPs.size()));
			}

			// Actually set who we're consuming from
			consumerProviders[consumer_i] = nsp_max_u_i;

			// Update the count of customers per NSP, important because
			// utility calculation changes with usage.
			// ignore -1s; no NSP
			if (consumerProviders[consumer_i] >= 0) {
				nsps[consumerProviders[consumer_i]].numCustomers++; // count
			}
			
		} // end each consumer

		for (int consumer_i = 0; consumer_i < numConsumerAgents; consumer_i++) {
			// Update consumer surplus
			if (consumerProviders[consumer_i] >= 0) {
				float surplus = consumerSurplus(consumerProviders[consumer_i],
						consumer_i);
				consumerSurplus[consumer_i] += surplus;
			}

		}

	}

	void consumerDecisions_RationalMomentum() {
		// Need to update # of consumers on each provider
		for (int i = 0; i < nsps.length; i++)
			nsps[i].numCustomers = 0; // reset

		// Each consumer
		for (int consumer_i = 0; consumer_i < numConsumerAgents; consumer_i++) {
			float u[] = new float[nsps.length];
			byte nsp_max_u_i = -1;
			float nsp_max_u = -1f;

			// Determine utilities for each NSP and which is best.
			List<Byte> bestNSPs = new ArrayList<Byte>();
			for (byte nsp_i = 0; nsp_i < nsps.length; nsp_i++) {
				u[nsp_i] = consumerSurplus(nsp_i, consumer_i);
				if (u[nsp_i] > nsp_max_u) {
					bestNSPs.clear();
					bestNSPs.add(nsp_i);
					nsp_max_u_i = nsp_i;
					nsp_max_u = u[nsp_i];
				} else if (u[nsp_i] == nsp_max_u) {
					bestNSPs.add(nsp_i);
				}
			}
			// If > 1 is best, choose each with equal probability
			if (bestNSPs.size() == 0) {
				nsp_max_u_i = -1;
			} else {
				nsp_max_u_i = bestNSPs.get(random.nextInt(bestNSPs.size()));
			}

			// Decide which NSP to consume from (or none) and do it
			float current_u = 0f;
			if (consumerProviders[consumer_i] >= 0)
				current_u = u[consumerProviders[consumer_i]];

			if (nsp_max_u <= 0) { // nobody has an attractive product
				consumerProviders[consumer_i] = -1;
			} else if (current_u <= 0) { // cur. is <=0, max is >0
				consumerProviders[consumer_i] = nsp_max_u_i;
			} else if (nsp_max_u >= current_u) { // someone is better than my
													// NSP
				// should I switch?
				// TODO: document in paper
				float utility_improvement = nsp_max_u - current_u;
				float percent_improvement = utility_improvement / 10;
				double switch_chance = s(percent_improvement);
				boolean change = random.nextBoolean(switch_chance);
				if (change)
					consumerProviders[consumer_i] = nsp_max_u_i;
			} // else my current NSP is the best option

			// ignore -1s; no NSP
			if (consumerProviders[consumer_i] >= 0) {
				// Update the count of NSPs
				nsps[consumerProviders[consumer_i]].numCustomers++; // count
			}
		}

		for (int consumer_i = 0; consumer_i < numConsumerAgents; consumer_i++) {
			// Update consumer surplus
			if (consumerProviders[consumer_i] >= 0) {
				float surplus = consumerSurplus(consumerProviders[consumer_i],
						consumer_i);
				consumerSurplus[consumer_i] += surplus;
			}

		}

	}

	float consumerSurplus(int nsp_i, int consumer_i) {
		float capacityFactor = nsps[nsp_i].capacityRatio();
		if (capacityFactor > 1)
			capacityFactor = 1;

		if (capacityFactor < 1)
			capacityFactor = (float) Math.pow(capacityFactor,
					congest_penalty_exp);

		float utility = capacityFactor * consumerWTP[consumer_i]
				- nsps[nsp_i].price;
		return utility;
	}

	float cumulativeConsumerSurplus() {
		double totalConsumerSurplus = 0d;
		for (int i = 0; i < numConsumerAgents; i++)
			totalConsumerSurplus += consumerSurplus[i];
		return (float) totalConsumerSurplus;
	}

	float cumulativeProducerSurplus() {
		double totalProducerSurplus = 0d;
		for (int i = 0; i < nsps.length; i++)
			totalProducerSurplus += nsps[i].getMoney();
		return (float) totalProducerSurplus;
	}

	double s(float priceDiffPercent) {
		return 1 / (1 + (Math.exp(((s_alpha) - priceDiffPercent) / s_beta)));
	}

	@Override
	public Map<Individual, Fitness> getFitnesses() {
		Map<Individual, Fitness> fitness = new IdentityHashMap<Individual, Fitness>();
		for (NSPAgent nsp : nsps) {
			SimpleFitness fit = new SimpleFitness();
			fit.setFitness(null, (float) nsp.money, false);

			fitness.put((Individual) nsp.ind, fit);
		}

		// System.out.print("F");

		return fitness;
	}

	public float getLowPrice() {
		float minPrice = Float.MAX_VALUE;
		for (NSPAgent nsp : nsps) {
			if (nsp.price < minPrice)
				minPrice = nsp.price;
		}
		return minPrice;
	}

	void checkReadyToStart() {
		if (modelID == null)
			throw new IllegalStateException("Model ID not set");
		if (generation == null)
			throw new IllegalStateException("Generation not set");
		if (randomSeed == null)
			throw new IllegalStateException("Random seed not initialized");
		if (random == null)
			throw new IllegalStateException("Randomizer not initialized");
		if (evalGroup == null)
			throw new IllegalStateException("No Evaluation Group to Evaluate");
		if (evalGroup.individuals == null)
			throw new IllegalStateException(
					"Evaluation group contains no individuals");
		if (evalGroup.individuals.isEmpty())
			throw new IllegalStateException(
					"Evaluation group contains no individuals");
		if (nsps == null)
			throw new IllegalStateException("NSPs not properly initialized");
		if (consumerWTP == null)
			throw new IllegalStateException(
					"Consumers not properly initialized");
		if (consumerProviders == null)
			throw new IllegalStateException(
					"Consumers not properly initialized");

	}

	@Override
	public void setSeed(int seed) {
		this.randomSeed = seed;
		random = new MersenneTwister(randomSeed);
	}

	@Override
	public void setEvaluationGroup(EvaluationGroup evalGroup) {
		this.evalGroup = evalGroup;

		int numBroadbandProviders = evalGroup.individuals.size();
		nsps = new NSPAgent[numBroadbandProviders];

		for (int i = 0; i < numBroadbandProviders; i++) {
			Individual ind = evalGroup.individuals.get(i);
			if (!(ind instanceof NSPIndividual))
				throw new IllegalArgumentException(
						"Individuals to be evaluated must implement NSPIndividual");
			nsps[i] = new NSPAgent(this, (NSPIndividual) ind);
		}

	}

	@Override
	public void setGeneration(Integer generation) {
		this.generation = generation;
	}

	@Override
	public Integer getGeneration() {
		return generation;
	}

	@Override
	public void setSimulationID(Integer simulationID) {
		this.modelID = simulationID;
	}

	@Override
	public Integer getSimulationID() {
		return this.modelID;
	}

}
