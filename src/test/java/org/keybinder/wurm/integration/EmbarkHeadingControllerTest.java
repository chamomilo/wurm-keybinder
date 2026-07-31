package org.keybinder.wurm.integration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmbarkHeadingControllerTest {
    @Test
    public void usesAuthoritativeServerVehicleRotation() {
        FailureCounter failures = new FailureCounter();
        EmbarkHeadingController controller = new EmbarkHeadingController(true, failures);
        FakeEnvironment environment = new FakeEnvironment();

        controller.align(environment, 90.0f);

        assertEquals(1, environment.writes);
        assertEquals(90.0f, environment.heading, 0.0f);
        assertEquals(0, failures.count);
    }

    @Test
    public void normalizesServerVehicleRotation() {
        EmbarkHeadingController controller =
                new EmbarkHeadingController(true, new FailureCounter());
        FakeEnvironment environment = new FakeEnvironment();

        controller.align(environment, -30.0f);
        assertEquals(330.0f, environment.heading, 0.0f);
        assertEquals(1, environment.writes);

        controller.align(environment, 725.0f);
        assertEquals(5.0f, environment.heading, 0.0f);
        assertEquals(2, environment.writes);
    }

    @Test
    public void disabledSettingDoesNothing() {
        EmbarkHeadingController controller =
                new EmbarkHeadingController(false, new FailureCounter());
        FakeEnvironment environment = new FakeEnvironment();

        controller.align(environment, 45.0f);

        assertEquals(0, environment.writes);
        assertFalse(controller.isActive());
    }

    @Test
    public void failureIsReportedOnceAndFeatureFailsOpen() {
        FailureCounter failures = new FailureCounter();
        EmbarkHeadingController controller = new EmbarkHeadingController(true, failures);
        FakeEnvironment environment = new FakeEnvironment();
        environment.failure = new IllegalStateException("broken client access");

        controller.align(environment, 10.0f);
        controller.align(environment, 20.0f);
        controller.align(environment, 30.0f);

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
        controller.align(environment, 10.0f);
        assertFalse(controller.isActive());

        environment.failure = null;
        controller.reset(true);
        controller.align(environment, 20.0f);

        assertTrue(controller.isActive());
        assertEquals(1, environment.writes);
    }

    @Test
    public void nonFiniteVehicleRotationFailsOpen() {
        FailureCounter failures = new FailureCounter();
        EmbarkHeadingController controller = new EmbarkHeadingController(true, failures);
        FakeEnvironment environment = new FakeEnvironment();

        controller.align(environment, Float.NaN);

        assertEquals(1, failures.count);
        assertEquals(0, environment.writes);
        assertFalse(controller.isActive());
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
        private float heading;
        private int writes;
        private RuntimeException failure;

        @Override public void setPlayerHeading(float value) {
            if (failure != null) throw failure;
            heading = value;
            writes++;
        }
    }
}
