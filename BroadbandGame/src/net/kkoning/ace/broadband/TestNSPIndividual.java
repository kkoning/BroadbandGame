package net.kkoning.ace.broadband;

import ec.util.Parameter;

/**
 * This individual always returns a fixed, simple amount.  It is useful for making sure
 * the simulation runs correctly when agent inputs are trivially simple.
 * 
 * @author kkoning
 *
 */
public class TestNSPIndividual extends ec.Individual implements NSPIndividual {
	private static final long serialVersionUID = 1L;

	@Override
	public float decidePrice(BroadbandModel m, NSPAgent a) {
		// Always set a price of 10
		return 120;
	}

	@Override
	public int setCapacity(BroadbandModel m, NSPAgent a) {
		return 1200;
	}

	@Override
	public Parameter defaultBase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean equals(Object ind) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}


}
