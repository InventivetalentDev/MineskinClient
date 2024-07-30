package org.mineskin;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import java.util.Map;

public class GenerateOptions {

    private String name;
    private Variant variant;
    private Visibility visibility;

    private GenerateOptions() {
    }

    public GenerateOptions name(String name) {
        this.name = name;
        return this;
    }

    public static GenerateOptions create() {
        return new GenerateOptions();
    }

    public GenerateOptions variant(Variant variant) {
        this.variant = variant;
        return this;
    }

    public GenerateOptions visibility(Visibility visibility) {
        this.visibility = visibility;
        return this;
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

    protected void addTo(Map<String, String> data) {
        if (!Strings.isNullOrEmpty(name)) {
            data.put("name", name);
        }
        if (variant != null && variant != Variant.AUTO) {
            data.put("variant", variant.getName());
        }
        if (visibility != null) {
            data.put("visibility", String.valueOf(visibility.getCode()));
        }
    }

    public String getName() {
        return name;
    }

}
