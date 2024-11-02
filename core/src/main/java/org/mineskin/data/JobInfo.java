package org.mineskin.data;

import java.util.Optional;

public class JobInfo {

    private final JobStatus status;
    private final long timestamp;
    private final String result;

    public JobInfo(JobStatus status, long timestamp, String result) {
        this.status = status;
        this.timestamp = timestamp;
        this.result = result;
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
