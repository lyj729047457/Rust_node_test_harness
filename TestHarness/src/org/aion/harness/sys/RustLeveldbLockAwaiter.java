package org.aion.harness.sys;

import java.util.Collections;
import java.util.List;

public class RustLeveldbLockAwaiter extends LeveldbLockAwaiter {
    private static final List<String> LOCK_FILES = Collections.unmodifiableList(
        List.of(
            "extra/LOCK",
            "state/LOCK",
            "bodies/LOCK",
            "headers/LOCK",
            "account_bloom/LOCK",
            "node_info/LOCK"
        ));

    /**
     * Constructor
     *
     * @param databaseDir root of the dir of the Aion database dir we're waiting on
     */
    public RustLeveldbLockAwaiter(String databaseDir) {
        super(databaseDir);
    }

    @Override
    protected List<String> getLockFiles() {
        return LOCK_FILES;
    }
}
