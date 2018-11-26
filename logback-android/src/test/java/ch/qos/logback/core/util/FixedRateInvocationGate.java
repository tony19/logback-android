package ch.qos.logback.core.util;

public class FixedRateInvocationGate implements InvocationGate {
    @Override
    public boolean isTooSoon(long currentTime) {
        return false;
    }
}