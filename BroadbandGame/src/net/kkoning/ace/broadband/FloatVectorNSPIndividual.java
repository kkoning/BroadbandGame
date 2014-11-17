package net.kkoning.ace.broadband;

public class FloatVectorNSPIndividual extends ec.vector.FloatVectorIndividual implements NSPIndividual {
	private static final long serialVersionUID = 1L;

	@Override
	public float decidePrice(BroadbandModel m, NSPAgent a) {
		return genome[0];
	}

	@Override
	public int setCapacity(BroadbandModel m, NSPAgent a) {
		return (int) (genome[1] * 10);
	}

}
