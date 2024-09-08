package org.mineskin.data;

public final class ExistingSkin extends BaseSkin {

    public ExistingSkin(
            String uuid,
            String name,
            SkinData data,
            long timestamp,
            int visibility,
            int views
    ) {
      super(uuid, name, data, timestamp, visibility, views);
    }

}
