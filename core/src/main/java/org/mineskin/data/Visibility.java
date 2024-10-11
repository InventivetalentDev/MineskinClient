package org.mineskin.data;

public enum Visibility {

	PUBLIC("public"),
	UNLISTED("unlisted"),
	PRIVATE("private");

	private final String name;

	Visibility(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
