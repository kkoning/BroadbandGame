package net.kkoning.ace.broadband;

public interface NSPIndividual {
	float decidePrice(BroadbandModel m, NSPAgent a);
	int setCapacity(BroadbandModel m, NSPAgent a);
}
