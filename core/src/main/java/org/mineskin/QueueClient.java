package org.mineskin;

import org.mineskin.data.JobInfo;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.JobResponse;
import org.mineskin.response.QueueResponse;

import java.util.concurrent.CompletableFuture;

public interface QueueClient {

    CompletableFuture<QueueResponse> submit(GenerateRequest builder);

    CompletableFuture<JobResponse> get(JobInfo jobInfo);

    CompletableFuture<JobResponse> get(String id);

    CompletableFuture<JobResponse> waitForCompletion(JobInfo jobInfo);

}
