package net.kkoning.ace.broadband;

public class SmartFloatVectorNSPIndividual extends FloatVectorNSPIndividual {

	@Override
	public float decidePrice(BroadbandModel m, NSPAgent a) {
		float targetPrice = super.decidePrice(m, a);
		if (a.numCustomers < a.networkCapacity)
			return m.getLowPrice();
		else
			return targetPrice;
	}

}
