package org.keybinder.wurm.recording;

/** Tracks whether a mouse press/release pair is an intentional target click. */
public final class TargetClickGesture {
    private static final int MAX_MOVEMENT = 3;
    private static final long MAX_DURATION_MILLIS = 1000L;

    private boolean armed;
    private boolean dragged;
    private int pressX;
    private int pressY;
    private long pressedAt;

    public synchronized void press(int x, int y, long now) {
        armed = true;
        dragged = false;
        pressX = x;
        pressY = y;
        pressedAt = now;
    }

    public synchronized void dragged() {
        if (armed) dragged = true;
    }

    public synchronized boolean matches(
            int releaseX, int releaseY, long now,
            boolean mouseLooking, boolean draggedInMouseLooking) {
        return isCandidate(releaseX, releaseY, now)
                && !mouseLooking && !draggedInMouseLooking;
    }

    public synchronized boolean isCandidate(int releaseX, int releaseY, long now) {
        return armed && !dragged
                && Math.abs(releaseX - pressX) <= MAX_MOVEMENT
                && Math.abs(releaseY - pressY) <= MAX_MOVEMENT
                && now - pressedAt <= MAX_DURATION_MILLIS;
    }

    public synchronized void reset() {
        armed = false;
        dragged = false;
        pressedAt = 0L;
    }
}
