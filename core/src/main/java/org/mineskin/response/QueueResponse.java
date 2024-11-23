package org.mineskin.response;

import org.mineskin.data.JobInfo;
import org.mineskin.data.RateLimitInfo;
import org.mineskin.data.UsageInfo;

public interface QueueResponse extends MineSkinResponse<JobInfo> {
    JobInfo getJob();

    RateLimitInfo getRateLimit();

    UsageInfo getUsage();
}
