package org.mineskin.response;

import org.mineskin.data.JobInfo;

import java.util.List;

public interface JobListResponse extends MineSkinResponse<List<JobInfo>> {
    List<JobInfo> getJobs();
}
