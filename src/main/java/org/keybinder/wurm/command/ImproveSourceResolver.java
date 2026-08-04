package org.keybinder.wurm.command;

import java.util.ArrayDeque;
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
        return resolve(toolbelt, inventoryRoot, builtIn, requirement, true, debug);
    }

    public ResolvedImproveResource resolve(
            List<ImproveResourceCandidate> toolbelt,
            ImproveResourceCandidate inventoryRoot,
            ImproveResourceCandidate builtIn,
            ResourceRequirement requirement,
            boolean allowInventory,
            Consumer<String> debug) {
        if (requirement == null) return null;
        Set<Long> visited = new HashSet<Long>();
        if (toolbelt != null) {
            for (ImproveResourceCandidate candidate : toolbelt) {
                if (candidate == null || !visited.add(candidate.getId())) continue;
                ResourceMatch match = requirement.match(candidate);
                debug(debug, "toolbelt", candidate, match);
                if (match.isAccepted())
                    return ResolvedImproveResource.toolbelt(candidate, requirement);
            }

            ArrayDeque<ToolbeltNode> pending = new ArrayDeque<ToolbeltNode>();
            for (ImproveResourceCandidate root : toolbelt) {
                if (root == null) continue;
                String container = display(root);
                for (ImproveResourceCandidate child : root.getChildren())
                    if (child != null) pending.addLast(new ToolbeltNode(child, container));
            }
            while (!pending.isEmpty()) {
                ToolbeltNode node = pending.removeFirst();
                ImproveResourceCandidate candidate = node.candidate;
                if (!visited.add(candidate.getId())) continue;
                ResourceMatch match = requirement.match(candidate);
                debug(debug, "toolbelt container \"" + node.containerName + "\"",
                        candidate, match);
                if (match.isAccepted())
                    return ResolvedImproveResource.toolbelt(
                            candidate, node.containerName, requirement);
                for (ImproveResourceCandidate child : candidate.getChildren())
                    if (child != null)
                        pending.addLast(new ToolbeltNode(child, node.containerName));
            }
        }

        if (requirement.getFamily() == RequirementFamily.BODY_HAND) {
            ResourceMatch match = requirement.match(builtIn);
            debug(debug, "built-in", builtIn, match);
            return match.isAccepted()
                    ? ResolvedImproveResource.builtIn(builtIn, requirement) : null;
        }
        if (!allowInventory) return null;
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

    private static String display(ImproveResourceCandidate candidate) {
        return candidate.getDisplayName().isEmpty()
                ? candidate.getBaseName() : candidate.getDisplayName();
    }

    private static final class ToolbeltNode {
        private final ImproveResourceCandidate candidate;
        private final String containerName;

        private ToolbeltNode(ImproveResourceCandidate candidate, String containerName) {
            this.candidate = candidate;
            this.containerName = containerName;
        }
    }
}
