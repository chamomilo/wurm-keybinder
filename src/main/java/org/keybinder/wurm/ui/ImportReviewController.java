package org.keybinder.wurm.ui;

import org.keybinder.wurm.bind.BindSnapshot;

import java.util.List;

public interface ImportReviewController {
    void confirmImport(List<BindSnapshot> selected);
    void closeImportReview(boolean doNotAskAgain);
}
