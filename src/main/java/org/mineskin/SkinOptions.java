package org.mineskin;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import org.jsoup.Connection;

public class SkinOptions {

    private static final String URL_FORMAT = "name=%s&model=%s&visibility=%s";

    private final String name;
    private final Variant variant;
    private final Visibility visibility;

    @Deprecated
    private SkinOptions(String name, Model model, Visibility visibility) {
        this.name = name;
        this.variant = model.toVariant();
        this.visibility = visibility;
    }

    private SkinOptions(String name, Variant variant, Visibility visibility) {
        this.name = name;
        this.variant = variant;
        this.visibility = visibility;
    }

    @Deprecated
    protected String toUrlParam() {
        return String.format(URL_FORMAT, this.name, this.variant.getName(), this.visibility.getCode());
    }

    protected JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (!Strings.isNullOrEmpty(name)) {
            json.addProperty("name", name);
        }
        if (variant != null && variant != Variant.AUTO) {
            json.addProperty("variant", variant.getName());
        }
        if (visibility != null) {
            json.addProperty("visibility", visibility.getCode());
        }
        return json;
    }

    protected void addAsData(Connection connection) {
        if (!Strings.isNullOrEmpty(name)) {
            connection.data("name", name);
        }
        if (variant != null && variant != Variant.AUTO) {
            connection.data("variant", variant.getName());
        }
        if (visibility != null) {
            connection.data("visibility", String.valueOf(visibility.getCode()));
        }
    }


    @Deprecated
    public static SkinOptions create(String name, Model model, Visibility visibility) {
        return new SkinOptions(name, model, visibility);
    }

    public static SkinOptions create(String name, Variant variant, Visibility visibility) {
        return new SkinOptions(name, variant, visibility);
    }

    public static SkinOptions name(String name) {
        return new SkinOptions(name, Variant.AUTO, Visibility.PUBLIC);
    }


    public static SkinOptions none() {
        return new SkinOptions("", Variant.AUTO, Visibility.PUBLIC);
    }

}
