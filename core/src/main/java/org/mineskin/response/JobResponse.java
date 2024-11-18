package org.mineskin.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mineskin.data.JobInfo;

import java.util.Map;

public class JobResponse extends MineSkinResponse<JobInfo> {

    public JobResponse(int status, Map<String, String> headers, JsonObject rawBody, Gson gson, Class<JobInfo> clazz) {
        super(status, headers, rawBody, gson, "job", clazz);
    }

}
