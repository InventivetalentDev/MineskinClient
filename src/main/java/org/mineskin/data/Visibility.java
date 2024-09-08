package org.mineskin.data;

public enum Visibility {

	PUBLIC(0),
	UNLISTED(1);

	private final int code;

	Visibility(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
