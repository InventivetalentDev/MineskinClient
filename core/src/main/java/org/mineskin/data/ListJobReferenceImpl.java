package org.mineskin.data;

import java.util.Optional;

public class ListJobReferenceImpl implements JobReference {

    private final JobInfo job;

    public ListJobReferenceImpl(JobInfo job) {
        this.job = job;
    }

    @Override
    public JobInfo getJob() {
        return job;
    }

    @Override
    public Optional<SkinInfo> getSkin() {
        return Optional.empty();
    }
}
