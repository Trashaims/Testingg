package com.zcore.core.system.pm.installer;

import com.zcore.core.env.BEnvironment;
import com.zcore.core.system.pm.BPackageSettings;
import com.zcore.entity.pm.InstallOption;
import com.zcore.utils.FileUtils;

public class RemoveAppExecutor implements Executor {
    public int exec(BPackageSettings ps, InstallOption option, int userId) {
        FileUtils.deleteDir(BEnvironment.getAppDir(ps.pkg.packageName));
        return 0;
    }
}
