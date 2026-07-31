package org.keybinder.wurm.integration;

import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;

import java.lang.reflect.Field;

/**
 * Detects a new carrier and aligns the driver's horizontal view immediately
 * after the player tick which established controller ownership.
 */
public final class EmbarkHeadingController {
    private static final int CONTROLLER_GRACE_TICKS = 2;

    public interface Environment {
        Object getCarrier() throws Exception;
        boolean isCarrierController() throws Exception;
        float getCarrierYaw(Object carrier) throws Exception;
        void setPlayerHeading(float heading) throws Exception;
    }

    public interface FailureHandler {
        void onFailure(Throwable failure);
    }

    private final FailureHandler failureHandler;
    private boolean configuredEnabled;
    private boolean failed;
    private Object observedCarrier;
    private Object pendingControllerCarrier;
    private int pendingControllerChecks;
    private HeadingFields headingFields;

    public EmbarkHeadingController(boolean enabled, FailureHandler failureHandler) {
        this.failureHandler = failureHandler;
        reset(enabled);
    }

    /**
     * Starts a fresh HUD session. A previous reflection failure disables only
     * the previous session, so the client signature is checked again here.
     */
    public void initializeClientAccess(boolean enabled) {
        reset(enabled);
        if (!enabled) return;
        try {
            headingFields = HeadingFields.resolve();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    public void reset(boolean enabled) {
        configuredEnabled = enabled;
        failed = false;
        clearCarrierState();
        headingFields = null;
    }

    public void cancel() {
        clearCarrierState();
    }

    private void clearCarrierState() {
        observedCarrier = null;
        clearPendingController();
    }

    private void clearPendingController() {
        pendingControllerCarrier = null;
        pendingControllerChecks = 0;
    }

    public boolean isActive() {
        return configuredEnabled && !failed;
    }

    public void disableAfterFailure(Throwable failure) {
        fail(failure);
    }

    public void tick(Environment environment) {
        if (!isActive() || environment == null) return;
        try {
            Object carrier = environment.getCarrier();
            update(environment, carrier);
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    public void tick(final PlayerObj player) {
        final HeadingFields fields = headingFields;
        if (!isActive() || player == null || fields == null) return;
        tick(new Environment() {
            @Override public Object getCarrier() {
                return player.getCarrierCreature();
            }

            @Override public boolean isCarrierController() {
                return player.isCarrierController();
            }

            @Override public float getCarrierYaw(Object carrier) {
                return ((CreatureCellRenderable) carrier).getYawValue(1.0f);
            }

            @Override public void setPlayerHeading(float heading)
                    throws IllegalAccessException {
                fields.setHeading(player, heading);
            }
        });
    }

    private void update(Environment environment, Object carrier) throws Exception {
        if (carrier == null) {
            cancel();
            return;
        }

        if (carrier != observedCarrier) {
            observedCarrier = carrier;
            clearPendingController();
            if (environment.isCarrierController()) {
                align(environment, carrier);
            } else {
                // Some server flows attach first and grant controller status
                // in a following update. Keep a short grace period without
                // ever turning a carrier that remains a passenger seat.
                pendingControllerCarrier = carrier;
                pendingControllerChecks = CONTROLLER_GRACE_TICKS;
            }
            return;
        }
        if (carrier == pendingControllerCarrier) {
            if (environment.isCarrierController()) {
                clearPendingController();
                align(environment, carrier);
            } else if (--pendingControllerChecks <= 0) {
                clearPendingController();
            }
        }
    }

    private void align(Environment environment, Object carrier) throws Exception {
        float yaw = environment.getCarrierYaw(carrier);
        if (Float.isNaN(yaw) || Float.isInfinite(yaw))
            throw new IllegalArgumentException("Carrier yaw is not finite: " + yaw);
        environment.setPlayerHeading(normalize(yaw));
    }

    static float normalize(float heading) {
        float normalized = heading % 360.0f;
        return normalized < 0.0f ? normalized + 360.0f : normalized;
    }

    private void fail(Throwable failure) {
        if (failed) return;
        failed = true;
        clearPendingController();
        if (failureHandler != null) failureHandler.onFailure(failure);
    }

    /**
     * Isolates the two private fields whose signatures were confirmed against
     * the pinned client. Resolving a new instance for every HUD session makes
     * reflection failures local to that session.
     */
    private static final class HeadingFields {
        private final Field playerHeading;
        private final Field carrierHeading;

        private HeadingFields(Field playerHeading, Field carrierHeading) {
            this.playerHeading = playerHeading;
            this.carrierHeading = carrierHeading;
        }

        private static HeadingFields resolve() throws ReflectiveOperationException {
            return new HeadingFields(
                    requiredFloatField("xRotUsed"),
                    requiredFloatField("xCarrierRotUsed"));
        }

        private static Field requiredFloatField(String name)
                throws ReflectiveOperationException {
            Field field = PlayerObj.class.getDeclaredField(name);
            if (field.getType() != Float.TYPE)
                throw new NoSuchFieldException("PlayerObj." + name + " is not float");
            field.setAccessible(true);
            return field;
        }

        private void setHeading(PlayerObj player, float heading)
                throws IllegalAccessException {
            playerHeading.setFloat(player, heading);
            // PlayerObj.gametick applies the carrier rotation delta to
            // xRotUsed. Keep its private baseline in sync so the heading above
            // is not compensated away on the next tick.
            CreatureCellRenderable carrier = player.getCarrierCreature();
            if (carrier != null) carrierHeading.setFloat(player, carrier.getRot());
        }
    }
}
