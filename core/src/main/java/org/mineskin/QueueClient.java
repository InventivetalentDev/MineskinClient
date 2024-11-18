package org.mineskin;

import org.mineskin.data.JobInfo;
import org.mineskin.request.RequestBuilder;
import org.mineskin.response.JobResponse;
import org.mineskin.response.QueueResponse;

import java.util.concurrent.CompletableFuture;

public interface QueueClient {

    CompletableFuture<QueueResponse> submit(RequestBuilder builder);

    CompletableFuture<JobResponse> get(JobInfo jobInfo);

    CompletableFuture<JobResponse> get(String id);

    CompletableFuture<JobInfo> waitForCompletion(JobInfo jobInfo);

}
