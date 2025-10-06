package org.mineskin.request.backoff;

public interface RequestInterval {
    /**
     * @param attempt attempt number, starting from 1
     * @return interval in milliseconds
     */
    int getInterval(int attempt);

    static RequestInterval constant(int intervalMillis) {
        return attempt -> intervalMillis;
    }

    static ExponentialBackoff exponential() {
        return new ExponentialBackoff(200, 2000, 2, 3);
    }
}
