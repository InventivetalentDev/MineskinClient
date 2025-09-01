package org.mineskin.data;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class UserInfo implements User {

    @SerializedName("user")
    private final String uuid;
    private final JsonObject grants;

    private Grants grantsWrapper;

    public UserInfo(String uuid, JsonObject grants) {
        this.uuid = uuid;
        this.grants = grants;
    }

    @Override
    public String uuid() {
        return uuid;
    }

    public JsonObject rawGrants() {
        return grants;
    }

    @Override
    public Grants grants() {
        if (grantsWrapper == null) {
            grantsWrapper = new Grants(grants);
        }
        return grantsWrapper;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "uuid='" + uuid + '\'' +
                ", grants=" + grants() +
                '}';
    }
}
