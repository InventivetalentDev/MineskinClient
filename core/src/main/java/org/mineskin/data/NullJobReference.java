package org.mineskin.data;

import java.util.Optional;

public class NullJobReference implements JobReference{

    private final JobInfo job;

    public NullJobReference(JobInfo job) {
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

    @Override
    public String toString() {
        return "NullJobReference{" +
                "job=" + job +
                '}';
    }
}
