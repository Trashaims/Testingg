package com.zcore.core.system.pm.installer;

import com.zcore.core.system.pm.BPackageSettings;
import com.zcore.entity.pm.InstallOption;

public interface Executor {
    public static final String TAG = "InstallExecutor";

    int exec(BPackageSettings bPackageSettings, InstallOption installOption, int i);
}
