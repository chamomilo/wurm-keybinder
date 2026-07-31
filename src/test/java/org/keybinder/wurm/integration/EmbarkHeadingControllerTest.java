package org.keybinder.wurm.integration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmbarkHeadingControllerTest {
    @Test
    public void centersViewForDriverOnFirstObservedTick() {
        FailureCounter failures = new FailureCounter();
        EmbarkHeadingController controller = new EmbarkHeadingController(true, failures);
        FakeEnvironment environment = new FakeEnvironment();
        environment.carrier = new Object();
        environment.controller = true;
        environment.yaw = -30.0f;

        controller.tick(environment);

        assertEquals(1, environment.writes);
        assertEquals(330.0f, environment.heading, 0.0f);
        assertEquals(0, failures.count);
    }

    @Test
    public void passengerIsNotTurned() {
        EmbarkHeadingController controller =
                new EmbarkHeadingController(true, new FailureCounter());
        FakeEnvironment environment = new FakeEnvironment();
        environment.carrier = new Object();
        environment.controller = false;

        tick(controller, environment, 3);

        assertEquals(0, environment.writes);
    }

    @Test
    public void doesNotDelayAfterCarrierAppears() {
        EmbarkHeadingController controller =
                new EmbarkHeadingController(true, new FailureCounter());
        FakeEnvironment environment = new FakeEnvironment();
        environment.carrier = new Object();
        environment.controller = true;

        controller.tick(environment);

        assertEquals(1, environment.writes);
    }

    @Test
    public void alignsWhenControllerStatusArrivesOneTickAfterAttach() {
        EmbarkHeadingController controller =
                new EmbarkHeadingController(true, new FailureCounter());
        FakeEnvironment environment = new FakeEnvironment();
        environment.carrier = new Object();
        environment.controller = false;

        controller.tick(environment);
        assertEquals(0, environment.writes);
        environment.controller = true;
        controller.tick(environment);

        assertEquals(1, environment.writes);
    }

    @Test
    public void disembarkCancelsPendingTurn() {
        EmbarkHeadingController controller =
                new EmbarkHeadingController(true, new FailureCounter());
        FakeEnvironment environment = new FakeEnvironment();
        environment.carrier = new Object();
        environment.controller = false;

        controller.tick(environment);
        environment.carrier = null;
        tick(controller, environment, 3);

        assertEquals(0, environment.writes);
    }

    @Test
    public void doesNotTurnAgainForSameCarrier() {
        EmbarkHeadingController controller =
                new EmbarkHeadingController(true, new FailureCounter());
        FakeEnvironment environment = new FakeEnvironment();
        environment.carrier = new Object();
        environment.controller = true;

        tick(controller, environment, 8);

        assertEquals(1, environment.writes);
    }

    @Test
    public void carrierChangeCancelsOldPendingAndSchedulesNewCarrier() {
        EmbarkHeadingController controller =
                new EmbarkHeadingController(true, new FailureCounter());
        FakeEnvironment environment = new FakeEnvironment();
        Object first = new Object();
        Object second = new Object();
        environment.carrier = first;
        environment.controller = true;
        environment.yaw = 10.0f;

        controller.tick(environment);
        environment.carrier = second;
        environment.yaw = 725.0f;
        controller.tick(environment);

        assertEquals(2, environment.writes);
        assertEquals(5.0f, environment.heading, 0.0f);
    }

    @Test
    public void disabledSettingDoesNothing() {
        EmbarkHeadingController controller =
                new EmbarkHeadingController(false, new FailureCounter());
        FakeEnvironment environment = new FakeEnvironment();
        environment.carrier = new Object();
        environment.controller = true;

        tick(controller, environment, 5);

        assertEquals(0, environment.writes);
        assertFalse(controller.isActive());
    }

    @Test
    public void failureIsReportedOnceAndFeatureFailsOpen() {
        FailureCounter failures = new FailureCounter();
        EmbarkHeadingController controller = new EmbarkHeadingController(true, failures);
        FakeEnvironment environment = new FakeEnvironment();
        environment.failure = new IllegalStateException("broken client access");

        tick(controller, environment, 3);

        assertEquals(1, failures.count);
        assertFalse(controller.isActive());
        assertEquals(0, environment.writes);
    }

    @Test
    public void resetReenablesFeatureAfterFailure() {
        FailureCounter failures = new FailureCounter();
        EmbarkHeadingController controller = new EmbarkHeadingController(true, failures);
        FakeEnvironment environment = new FakeEnvironment();
        environment.failure = new IllegalStateException("broken client access");
        controller.tick(environment);
        assertFalse(controller.isActive());

        environment.failure = null;
        environment.carrier = new Object();
        environment.controller = true;
        controller.reset(true);
        controller.tick(environment);

        assertTrue(controller.isActive());
        assertEquals(1, environment.writes);
    }

    private static void tick(EmbarkHeadingController controller,
                             FakeEnvironment environment, int count) {
        for (int i = 0; i < count; i++) controller.tick(environment);
    }

    private static final class FailureCounter
            implements EmbarkHeadingController.FailureHandler {
        private int count;

        @Override public void onFailure(Throwable failure) {
            count++;
        }
    }

    private static final class FakeEnvironment
            implements EmbarkHeadingController.Environment {
        private Object carrier;
        private boolean controller;
        private float yaw;
        private float heading;
        private int writes;
        private RuntimeException failure;

        @Override public Object getCarrier() {
            if (failure != null) throw failure;
            return carrier;
        }

        @Override public boolean isCarrierController() {
            return controller;
        }

        @Override public float getCarrierYaw(Object requestedCarrier) {
            if (requestedCarrier != carrier)
                throw new IllegalStateException("stale carrier");
            return yaw;
        }

        @Override public void setPlayerHeading(float value) {
            heading = value;
            writes++;
        }
    }
}
