package org.mineskin;

import org.mineskin.data.JobInfo;
import org.mineskin.request.RequestBuilder;
import org.mineskin.response.JobResponse;
import org.mineskin.response.QueueResponse;

import java.util.concurrent.CompletableFuture;

public interface QueueClient {

    CompletableFuture<QueueResponse> queue(RequestBuilder builder);

    CompletableFuture<JobResponse> getJobStatus(JobInfo jobInfo);

    CompletableFuture<JobResponse> getJobStatus(String id);

}
