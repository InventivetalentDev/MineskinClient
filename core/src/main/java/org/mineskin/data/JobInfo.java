package org.mineskin.data;

import java.util.Optional;

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

}
