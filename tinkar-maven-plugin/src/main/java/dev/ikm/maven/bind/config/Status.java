package dev.ikm.maven.bind.config;

import dev.ikm.tinkar.coordinate.stamp.StateSet;
import dev.ikm.tinkar.terms.State;

import static dev.ikm.tinkar.coordinate.stamp.StateSet.make;

public enum Status {

	ACTIVE(State.ACTIVE),
	ACTIVE_AND_INACTIVE(State.ACTIVE, State.INACTIVE),
	ACTIVE_INACTIVE_AND_WITHDRAWN(State.ACTIVE, State.INACTIVE, State.WITHDRAWN),
	INACTIVE(State.INACTIVE),
	WITHDRAWN(State.WITHDRAWN);

	private State[] states;

	Status(State... states) {
		this.states = states;
	}


}
