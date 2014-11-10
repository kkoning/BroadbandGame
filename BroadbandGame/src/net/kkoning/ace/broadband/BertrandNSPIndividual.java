package net.kkoning.ace.broadband;

public class BertrandNSPIndividual extends ec.agency.NullIndividual implements NSPIndividual {
	private static final long serialVersionUID = 1L;

	@Override
	public float decidePrice(BroadbandModel m, NSPAgent a) {
		NSPAgent other = m.getOtherNSP(a);
		float y = m.numConsumerAgents - other.numCustomers;
		float price = 0.5f * y * m.perConsumerWTPReduction;
		return price;
	}

	@Override
	public int setCapacity(BroadbandModel m, NSPAgent a) {
		NSPAgent other = m.getOtherNSP(a);
		float y = m.numConsumerAgents - other.numCustomers;
		float b = 1 / m.perConsumerWTPReduction;
		float qty = y - b * a.price;

		return (int) qty;
	}

}
