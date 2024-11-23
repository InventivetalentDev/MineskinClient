package org.mineskin.response;

import org.mineskin.MineSkinClient;
import org.mineskin.data.JobInfo;
import org.mineskin.data.SkinInfo;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface JobResponse extends MineSkinResponse<JobInfo> {
    JobInfo getJob();

    Optional<SkinInfo> getSkin();

    CompletableFuture<SkinInfo> getOrLoadSkin(MineSkinClient client);
}
