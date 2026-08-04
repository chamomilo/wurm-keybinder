package org.keybinder.wurm.command;

public final class ResolvedImproveResource {
    private final ImproveResourceCandidate candidate;
    private final String containerName;
    private final ResourceRequirement requirement;
    private final boolean toolbelt;
    private final boolean builtIn;

    ResolvedImproveResource(ImproveResourceCandidate candidate,
                            String containerName,
                            ResourceRequirement requirement) {
        this(candidate, containerName, requirement, false, false);
    }

    private ResolvedImproveResource(ImproveResourceCandidate candidate,
                                    ResourceRequirement requirement,
                                    boolean toolbelt, boolean builtIn) {
        this(candidate, null, requirement, toolbelt, builtIn);
    }

    private ResolvedImproveResource(ImproveResourceCandidate candidate,
                                    String containerName,
                                    ResourceRequirement requirement,
                                    boolean toolbelt, boolean builtIn) {
        this.candidate = candidate;
        this.containerName = containerName;
        this.requirement = requirement;
        this.toolbelt = toolbelt;
        this.builtIn = builtIn;
    }

    static ResolvedImproveResource toolbelt(ImproveResourceCandidate candidate,
                                             ResourceRequirement requirement) {
        return new ResolvedImproveResource(candidate, requirement, true, false);
    }

    static ResolvedImproveResource toolbelt(ImproveResourceCandidate candidate,
                                             String containerName,
                                             ResourceRequirement requirement) {
        return new ResolvedImproveResource(candidate, containerName,
                requirement, true, false);
    }

    static ResolvedImproveResource builtIn(ImproveResourceCandidate candidate,
                                            ResourceRequirement requirement) {
        return new ResolvedImproveResource(candidate, requirement, false, true);
    }

    public ImproveResourceCandidate getCandidate() { return candidate; }
    public String getContainerName() { return containerName; }
    public boolean isNested() { return containerName != null; }
    public boolean isToolbelt() { return toolbelt; }
    public boolean isBuiltIn() { return builtIn; }
    public ResourceRequirement getRequirement() { return requirement; }
}
