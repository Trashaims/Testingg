package com.bgmi;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.util.Log;
import com.zcore.ZCoreCore;
import com.zcore.app.configuration.AppLifecycleCallback;
import com.zcore.app.configuration.ClientConfiguration;
import java.io.File;

import java.security.MessageDigest;
import com.bgmi.utils.Prefs;
import org.lsposed.lsparanoid.Obfuscate;
@Obfuscate
public class BoxApplication extends Application {

    static {
        System.loadLibrary("THUNDER"); 
    }
    
    public static native String getSdkKey();
    private static final String TAG = "BoxApplication";
    private static final String EXPECTED_SIGNATURE_HASH = "E545B00E31AF8FC008566DE0B87C0B17FC467A90AEED71B00755B98698AE58BC";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "BoxApplication attachBaseContext started");
        Prefs prefs = new Prefs(base);
        
        try {
            Log.d(TAG, "Initializing SboxCore...");
            ZCoreCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                @Override
                public boolean isEnableDaemonService() {
                    return false;
                }

                @Override
                public boolean requestInstallPackage(File file){
                    PackageInfo packageInfo = base.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),0);
                    return false;
                }
            });
            Log.d(TAG, "SboxCore initialization completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in attachBaseContext", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ZCoreCore.get().doCreate();
        // Check signature before activating
        if (isValidSignature()) {
            Log.d(TAG, "Valid signature found, activating box...");
            
        } else {
            Log.w(TAG, "Invalid signature! Box activation skipped.");

        }
        
        ZCoreCore.get().addAppLifecycleCallback(new AppLifecycleCallback() {
            @Override
            public void beforeCreateApplication(String packageName, String processName, Context context, int userId) {
            }

            @Override
            public void beforeApplicationOnCreate(String packageName, String processName, Application application, int userId) {
            }

            @Override
            public void afterApplicationOnCreate(String packageName, String processName, Application application, int userId) {
            }
        });
    }

    private boolean isValidSignature() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                getPackageName(), 
                PackageManager.GET_SIGNING_CERTIFICATES
            );
            
            Signature[] signatures = packageInfo.signingInfo.getApkContentsSigners();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            for (Signature signature : signatures) {
                byte[] digest = md.digest(signature.toByteArray());
                StringBuilder hash = new StringBuilder();
                
                for (byte b : digest) {
                    hash.append(String.format("%02X", b));
                }
                
                String currentHash = hash.toString();
                Log.d(TAG, "Current signature hash: " + currentHash);
                Log.d(TAG, "Expected signature hash: " + EXPECTED_SIGNATURE_HASH);
                
                if (EXPECTED_SIGNATURE_HASH.equals(currentHash)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking signature", e);
            e.printStackTrace();
        }
        
        return false;
    }
}
