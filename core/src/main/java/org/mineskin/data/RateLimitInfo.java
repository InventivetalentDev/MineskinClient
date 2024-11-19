package org.mineskin.data;

public record RateLimitInfo(NextRequest next, DelayInfo delay, LimitInfo limit) {
}
