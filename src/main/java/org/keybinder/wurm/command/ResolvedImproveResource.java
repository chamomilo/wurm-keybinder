package org.keybinder.wurm.command;

public final class ResolvedImproveResource {
    private final ImproveResourceCandidate candidate;
    private final String containerName;
    private final ResourceRequirement requirement;

    ResolvedImproveResource(ImproveResourceCandidate candidate,
                            String containerName,
                            ResourceRequirement requirement) {
        this.candidate = candidate;
        this.containerName = containerName;
        this.requirement = requirement;
    }

    public ImproveResourceCandidate getCandidate() { return candidate; }
    public String getContainerName() { return containerName; }
    public boolean isNested() { return containerName != null; }
    public ResourceRequirement getRequirement() { return requirement; }
}
