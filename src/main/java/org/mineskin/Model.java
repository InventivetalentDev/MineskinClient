package org.mineskin;

@Deprecated
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

    public Variant toVariant() {
        switch (this) {
			default:
            case DEFAULT:
                return Variant.CLASSIC;
            case SLIM:
                return Variant.SLIM;
        }
    }

}
