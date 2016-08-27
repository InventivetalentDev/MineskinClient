package org.mineskin;

public enum Model {

	DEFAULT("steve"),
	SLIM("slim");

	private final String name;

	Model(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
