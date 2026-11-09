package vn.edu.qnu.events.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class MutableClock extends Clock {

    private Instant currentInstant;
    private final ZoneId zone;

    public MutableClock(Instant initialInstant, ZoneId zone) {
        this.currentInstant = initialInstant;
        this.zone = zone;
    }

    public MutableClock(Instant initialInstant) {
        this(initialInstant, ZoneOffset.UTC);
    }

    public static MutableClock of(Instant instant) {
        return new MutableClock(instant);
    }

    public void setInstant(Instant instant) {
        this.currentInstant = instant;
    }

    public void plus(Duration duration) {
        this.currentInstant = this.currentInstant.plus(duration);
    }

    public void plusSeconds(long seconds) {
        this.currentInstant = this.currentInstant.plusSeconds(seconds);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(currentInstant, zone);
    }

    @Override
    public Instant instant() {
        return currentInstant;
    }
}

