package org.keybinder.wurm.command;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Shared Smart Improve source order: toolbelt, then the legacy fallback. */
public final class ImproveSourceResolver {
    private final ImproveResourceResolver inventory = new ImproveResourceResolver();

    public ResolvedImproveResource resolve(
            List<ImproveResourceCandidate> toolbelt,
            ImproveResourceCandidate inventoryRoot,
            ImproveResourceCandidate builtIn,
            ResourceRequirement requirement,
            Consumer<String> debug) {
        if (requirement == null) return null;
        Set<Long> visited = new HashSet<Long>();
        if (toolbelt != null)
            for (ImproveResourceCandidate candidate : toolbelt) {
                if (candidate == null || !visited.add(candidate.getId())) continue;
                ResourceMatch match = requirement.match(candidate);
                debug(debug, "toolbelt", candidate, match);
                if (match.isAccepted())
                    return ResolvedImproveResource.toolbelt(candidate, requirement);
            }

        if (requirement.getFamily() == RequirementFamily.BODY_HAND) {
            ResourceMatch match = requirement.match(builtIn);
            debug(debug, "built-in", builtIn, match);
            return match.isAccepted()
                    ? ResolvedImproveResource.builtIn(builtIn, requirement) : null;
        }
        return inventory.resolve(inventoryRoot, requirement, debug);
    }

    private static void debug(Consumer<String> debug, String source,
                              ImproveResourceCandidate candidate,
                              ResourceMatch match) {
        if (debug == null || candidate == null) return;
        debug.accept((match.isAccepted() ? "accepted " : "rejected ")
                + source + " id=" + candidate.getId()
                + " baseName=\"" + candidate.getBaseName() + "\": "
                + match.getReason());
    }
}
