package org.mineskin.data;

import org.mineskin.MineSkinClient;
import org.mineskin.exception.MineskinException;
import org.mineskin.response.SkinResponse;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class JobInfo implements MutableBreadcrumbed {

    private final String id;
    private final JobStatus status;
    private final long timestamp;
    private final long eta;
    private final String result;

    private String breadcrumb;

    public JobInfo(String id, JobStatus status, long timestamp, String result) {
        this(id, status, timestamp, 0, result);
    }

    public JobInfo(String id, JobStatus status, long timestamp, long eta, String result) {
        this.id = id;
        this.status = status;
        this.timestamp = timestamp;
        this.eta = eta;
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

    public long eta() {
        return eta;
    }

    public Optional<String> result() {
        return Optional.ofNullable(result);
    }

    @Nullable
    @Override
    public String getBreadcrumb() {
        return breadcrumb;
    }

    @Override
    public void setBreadcrumb(String breadcrumb) {
        this.breadcrumb = breadcrumb;
    }

    public CompletableFuture<JobReference> waitForCompletion(MineSkinClient client) {
        return client.queue().waitForCompletion(this);
    }

    public CompletableFuture<SkinResponse> getSkin(MineSkinClient client) {
        if (result == null) {
            throw new MineskinException("Job is not completed yet").withBreadcrumb(getBreadcrumb());
        }
        return client.skins().get(result);
    }

    @Override
    public String toString() {
        return "JobInfo{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", timestamp=" + timestamp +
                ", eta=" + eta +
                ", result='" + result + '\'' +
                ", breadcrumb='" + breadcrumb + '\'' +
                '}';
    }
}
