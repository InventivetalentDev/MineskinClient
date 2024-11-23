package org.mineskin.response;

import org.mineskin.data.SkinInfo;

public interface SkinResponse extends MineSkinResponse<SkinInfo> {
    SkinInfo getSkin();
}
