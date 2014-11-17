package net.kkoning.ace.broadband;

import ec.vector.FloatVectorIndividual;

public class CournotExpectationsNSPIndividual extends FloatVectorIndividual implements NSPIndividual {
	private static final long serialVersionUID = 1L;

	@Override
	public int setCapacity(BroadbandModel m, NSPAgent a) {
		return (int) (genome[0] * 10);
	}

	@Override
	public float decidePrice(BroadbandModel m, NSPAgent a) {
		int capacity = m.totalCapacity();
		// since customers are ordered in WTP, can just look it up
		if (capacity > m.consumerWTP.length)
			return 0f;
		
		if (capacity <= 0)
			return m.maxConsumerWTP;
		
		float clearingPrice = m.consumerWTP[capacity-1];
		float lowestPrice = m.getLowPrice();
		if (m.step == 0)
			lowestPrice = clearingPrice;
		float targetPrice = 0;
		if (lowestPrice < clearingPrice)
			targetPrice = lowestPrice;
		else
			targetPrice = clearingPrice;
		
		return targetPrice;
		
	}

	
	
	
}
