package net.kkoning.ace.broadband;

public class NSPAgent {
	NSPIndividual ind;
	Object ind_state;
	
	BroadbandModel model;
	
	double money;
	double income;
	double capCost;
	
	float price;
	int numCustomers;
	int networkCapacity;
	
	private static float MAX_CAPACITY_RATIO = 10;
	
	public NSPAgent(BroadbandModel model, NSPIndividual ind) {
		this.model = model;
		this.ind = ind;
	}
	
	float capacityRatio() {
		if (numCustomers == 0)
			return MAX_CAPACITY_RATIO;
		
		float capacityRatio = (float) networkCapacity / numCustomers;
		if (capacityRatio > MAX_CAPACITY_RATIO)
			return MAX_CAPACITY_RATIO;
		return capacityRatio;
	}
	
	void bill() {
		income = price * numCustomers;
		money += income;
	}
	
	public double getMoney() {
		return money;
	}
	
	public void decidePrice() {
		price = ind.decidePrice(model, this);
	}
	
	public void decideCapacity() {
		networkCapacity = ind.setCapacity(model, this);
		
	}

	public void capacityBill() {
		double capBill = networkCapacity * model.capacityCost;
		capCost += capBill;
		money -= capBill;
	}
	
}
