package org.mineskin;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import org.mineskin.data.Variant;
import org.mineskin.data.Visibility;

import java.util.HashMap;
import java.util.Map;

public class GenerateOptions {

    private String name;
    private Variant variant;
    private Visibility visibility;

    private GenerateOptions() {
    }

    public static GenerateOptions create() {
        return new GenerateOptions();
    }

    /**
     * Set the name of the skin (optional)
     */
    public GenerateOptions name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the variant of the skin (optional, defaults to auto-detect)
     */
    public GenerateOptions variant(Variant variant) {
        this.variant = variant;
        return this;
    }

    /**
     * Set the visibility of the skin (optional, defaults to public)
     */
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
            json.addProperty("visibility", visibility.getName());
        }
        return json;
    }

    protected Map<String, String> toMap() {
        Map<String, String> data = new HashMap<>();
        addTo(data);
        return data;
    }

    protected void addTo(Map<String, String> data) {
        if (!Strings.isNullOrEmpty(name)) {
            data.put("name", name);
        }
        if (variant != null && variant != Variant.AUTO) {
            data.put("variant", variant.getName());
        }
        if (visibility != null) {
            data.put("visibility", visibility.getName());
        }
    }

    public String getName() {
        return name;
    }

}
