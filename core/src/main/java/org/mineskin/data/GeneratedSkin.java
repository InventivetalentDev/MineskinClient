package org.mineskin.data;

@Deprecated
public class GeneratedSkin extends BaseSkin {

    private final boolean duplicate;

    public GeneratedSkin(
            String uuid,
            String name,
            SkinData data,
            long timestamp,
            Visibility visibility,
            int views,
            boolean duplicate
    ) {
        super(uuid, name, data, timestamp, visibility, views);
        this.duplicate = duplicate;
    }

    public boolean duplicate() {
        return duplicate;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                "uuid=" + uuid() + ", " +
                "name=" + name() + ", " +
                "data=" + data() + ", " +
                "timestamp=" + timestamp() + ", " +
                "visibility=" + visibility() + ", " +
                "views=" + views() + ", " +
                "duplicate=" + duplicate() + ']';
    }

}
