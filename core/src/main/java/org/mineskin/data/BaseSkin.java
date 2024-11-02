package org.mineskin.data;

import java.util.Objects;

@Deprecated
public class BaseSkin implements Skin {

    private final String uuid;
    private final String name;
    private final SkinData data;
    private final long timestamp;
    private final Visibility visibility;
    private final int views;

    public BaseSkin(
            String uuid,
            String name,
            SkinData data,
            long timestamp,
            Visibility visibility,
            int views
    ) {
        this.uuid = uuid;
        this.name = name;
        this.data = data;
        this.timestamp = timestamp;
        this.visibility = visibility;
        this.views = views;
    }

    @Override
    public String uuid() {
        return uuid;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public SkinData data() {
        return data;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public Visibility visibility() {
        return visibility;
    }

    @Override
    public int views() {
        return views;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BaseSkin) obj;
        return Objects.equals(this.uuid, that.uuid) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.data, that.data) &&
                this.timestamp == that.timestamp &&
                this.visibility == that.visibility &&
                this.views == that.views;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name, data, timestamp, visibility, views);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                "uuid=" + uuid + ", " +
                "name=" + name + ", " +
                "data=" + data + ", " +
                "timestamp=" + timestamp + ", " +
                "visibility=" + visibility + ", " +
                "views=" + views + ']';
    }

}
