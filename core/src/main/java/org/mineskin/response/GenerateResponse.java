package org.mineskin.response;

import org.mineskin.data.RateLimitInfo;
import org.mineskin.data.SkinInfo;
import org.mineskin.data.UsageInfo;

public interface GenerateResponse extends MineSkinResponse<SkinInfo> {
    SkinInfo getSkin();

    RateLimitInfo getRateLimit();

    UsageInfo getUsage();
}
