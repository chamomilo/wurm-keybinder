package org.keybinder.wurm.integration;

import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;

import java.lang.reflect.Field;

/**
 * Aligns the driver's horizontal view to the authoritative vehicle rotation
 * supplied by the server to {@link PlayerObj#setController}.
 */
public final class EmbarkHeadingController {
    public interface Environment {
        void setPlayerHeading(float heading) throws Exception;
    }

    public interface FailureHandler {
        void onFailure(Throwable failure);
    }

    private final FailureHandler failureHandler;
    private boolean configuredEnabled;
    private boolean failed;
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
        headingFields = null;
    }

    public boolean isActive() {
        return configuredEnabled && !failed;
    }

    public void disableAfterFailure(Throwable failure) {
        fail(failure);
    }

    public void align(Environment environment, float vehicleRotation) {
        if (!isActive() || environment == null) return;
        try {
            if (Float.isNaN(vehicleRotation) || Float.isInfinite(vehicleRotation))
                throw new IllegalArgumentException(
                        "Vehicle rotation is not finite: " + vehicleRotation);
            environment.setPlayerHeading(normalize(vehicleRotation));
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    public void align(final PlayerObj player, float vehicleRotation) {
        final HeadingFields fields = headingFields;
        if (!isActive() || player == null || fields == null) return;
        align(new Environment() {
            @Override public void setPlayerHeading(float heading)
                    throws IllegalAccessException {
                fields.setHeading(player, heading);
            }
        }, vehicleRotation);
    }

    static float normalize(float heading) {
        float normalized = heading % 360.0f;
        return normalized < 0.0f ? normalized + 360.0f : normalized;
    }

    private void fail(Throwable failure) {
        if (failed) return;
        failed = true;
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
