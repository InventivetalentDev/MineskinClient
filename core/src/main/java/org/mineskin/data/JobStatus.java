package org.mineskin.data;

import com.google.gson.annotations.SerializedName;

public enum JobStatus {
    @SerializedName("waiting")
    WAITING,
    @SerializedName("processing")
    PROCESSING,
    @SerializedName("completed")
    COMPLETED,
    @SerializedName("failed")
    FAILED,
    @SerializedName("unknown")
    UNKNOWN,
    ;

    public boolean isPending() {
        return this == WAITING || this == PROCESSING;
    }

    public boolean isDone() {
        return this == COMPLETED || this == FAILED;
    }

}
