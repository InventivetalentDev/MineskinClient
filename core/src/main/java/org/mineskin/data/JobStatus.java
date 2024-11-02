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
}
