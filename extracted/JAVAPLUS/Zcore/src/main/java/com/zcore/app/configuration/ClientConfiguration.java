package com.zcore.app.configuration;

import java.io.File;

public abstract class ClientConfiguration {
    public abstract String getHostPackageName();

  /*  public boolean isHideRoot() {
        return false;
    }

    public boolean isHideXposed() {
        return false;
    }*/
    
    public boolean setHideRoot() {
        return false;
    }

    public boolean isEnableDaemonService() {
        return true;
    }

    public boolean isEnableLauncherActivity() {
        return true;
    }

    public boolean requestInstallPackage(File file) {
        return false;
    }
}
