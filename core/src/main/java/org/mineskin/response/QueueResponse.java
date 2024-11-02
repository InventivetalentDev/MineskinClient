package org.mineskin.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mineskin.data.JobInfo;

import java.util.Map;

public class QueueResponse extends MineSkinResponse<JobInfo> {

    public QueueResponse(int status, Map<String, String> headers, JsonObject rawBody, Gson gson) {
        super(status, headers, rawBody, gson, "job", JobInfo.class);
    }

}
