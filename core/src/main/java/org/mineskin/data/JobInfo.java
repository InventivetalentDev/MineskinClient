package org.mineskin.data;

import org.mineskin.MineSkinClient;
import org.mineskin.exception.MineskinException;
import org.mineskin.response.SkinResponse;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class JobInfo {

    private final String id;
    private final JobStatus status;
    private final long timestamp;
    private final String result;

    public JobInfo(String id, JobStatus status, long timestamp, String result) {
        this.id = id;
        this.status = status;
        this.timestamp = timestamp;
        this.result = result;
    }

    public String id() {
        return id;
    }

    public JobStatus status() {
        return status;
    }

    public long timestamp() {
        return timestamp;
    }

    public Optional<String> result() {
        return Optional.ofNullable(result);
    }

    public CompletableFuture<JobReference> waitForCompletion(MineSkinClient client) {
        return client.queue().waitForCompletion(this);
    }

    public CompletableFuture<SkinResponse> getSkin(MineSkinClient client) {
        if (result == null) {
            throw new MineskinException("Job is not completed yet");
        }
        return client.skins().get(result);
    }

    @Override
    public String toString() {
        return "JobInfo{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", timestamp=" + timestamp +
                ", result='" + result + '\'' +
                '}';
    }
}
