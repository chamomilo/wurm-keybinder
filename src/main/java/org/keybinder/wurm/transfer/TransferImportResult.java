package org.keybinder.wurm.transfer;

public final class TransferImportResult {
    private final int imported;
    private final int skippedDuplicates;
    private final int rejected;

    public TransferImportResult(int imported, int skippedDuplicates, int rejected) {
        this.imported = imported;
        this.skippedDuplicates = skippedDuplicates;
        this.rejected = rejected;
    }

    public int getImported() { return imported; }
    public int getSkippedDuplicates() { return skippedDuplicates; }
    public int getRejected() { return rejected; }
}
