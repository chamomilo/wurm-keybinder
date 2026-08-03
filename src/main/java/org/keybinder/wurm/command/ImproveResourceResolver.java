package org.keybinder.wurm.command;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** One direct-first, then recursive pass over the player-inventory snapshot. */
public final class ImproveResourceResolver {
    public ResolvedImproveResource resolve(ImproveResourceCandidate inventoryRoot,
                                           ResourceRequirement requirement,
                                           Consumer<String> debug) {
        if (inventoryRoot == null || requirement == null) return null;
        Set<Long> visited = new HashSet<Long>();
        visited.add(inventoryRoot.getId());

        // Priority tier: items lying directly in inventory, followed by items
        // lying directly in a top-level backpack. Nested containers are searched
        // only if this tier contains no usable candidate.
        List<Node> direct = new ArrayList<Node>();
        for (ImproveResourceCandidate child : inventoryRoot.getChildren())
            if (child != null) direct.add(new Node(child, null));
        for (ImproveResourceCandidate child : inventoryRoot.getChildren()) {
            if (child == null || !isBackpack(child)) continue;
            String backpackName = display(child);
            for (ImproveResourceCandidate content : child.getChildren())
                if (content != null) direct.add(new Node(content, backpackName));
        }

        for (Node node : direct) {
            if (!visited.add(node.candidate.getId())) continue;
            ResourceMatch match = requirement.match(node.candidate);
            debug(debug, node.candidate, match);
            if (match.isAccepted())
                return new ResolvedImproveResource(node.candidate,
                        node.containerName, requirement);
        }

        ArrayDeque<Node> pending = new ArrayDeque<Node>();
        for (Node node : direct) {
            String childContainer = node.containerName == null
                    ? display(node.candidate) : node.containerName;
            for (ImproveResourceCandidate child : node.candidate.getChildren())
                if (child != null) pending.addLast(new Node(child, childContainer));
        }
        while (!pending.isEmpty()) {
            Node node = pending.removeFirst();
            ImproveResourceCandidate candidate = node.candidate;
            if (!visited.add(candidate.getId())) continue;
            ResourceMatch match = requirement.match(candidate);
            debug(debug, candidate, match);
            if (match.isAccepted())
                return new ResolvedImproveResource(candidate, node.containerName,
                        requirement);
            String childContainer = node.containerName == null
                    ? display(candidate) : node.containerName;
            for (ImproveResourceCandidate child : candidate.getChildren())
                if (child != null) pending.addLast(new Node(child, childContainer));
        }
        return null;
    }

    private static String display(ImproveResourceCandidate candidate) {
        return candidate.getDisplayName().isEmpty()
                ? candidate.getBaseName() : candidate.getDisplayName();
    }

    private static boolean isBackpack(ImproveResourceCandidate candidate) {
        return candidate != null && "backpack".equals(candidate.getBaseName());
    }

    private static void debug(Consumer<String> debug,
                              ImproveResourceCandidate candidate,
                              ResourceMatch match) {
        if (debug == null || candidate == null) return;
        debug.accept((match.isAccepted() ? "accepted " : "rejected ")
                + "id=" + candidate.getId()
                + " baseName=\"" + candidate.getBaseName() + "\""
                + " displayName=\"" + candidate.getDisplayName() + "\""
                + " image=" + candidate.getImageId()
                + " material=" + (candidate.getMaterialId() & 0xff)
                + " temperature=" + candidate.getTemperature()
                + ": " + match.getReason());
    }

    private static final class Node {
        private final ImproveResourceCandidate candidate;
        private final String containerName;

        private Node(ImproveResourceCandidate candidate, String containerName) {
            this.candidate = candidate;
            this.containerName = containerName;
        }
    }
}
