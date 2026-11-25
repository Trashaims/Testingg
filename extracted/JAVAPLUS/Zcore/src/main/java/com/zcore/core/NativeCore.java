package com.zcore.core;

import android.os.Process;
import android.util.Log;
import androidx.annotation.Keep;
import android.content.Context;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import com.zcore.ZCoreCore;
import com.zcore.app.BActivityThread;

public class NativeCore {
    public static final String TAG = "NativeCore";
    private static boolean isInjected = false;
    public static String libtarget = "libbgmi.so";
    
        static {
            System.loadLibrary("blackbox");
            if (!isInjected) {
                Context context = ZCoreCore.getContext();
                File libFile = new File(context.getFilesDir(), "loader/" + libtarget);
                if (libFile.exists()) {
                    System.load(libFile.getAbsolutePath());
                    isInjected = true;
                    Log.i(TAG, libtarget + " loaded");
                }
            }
        }
    
    private static void dumpMaps() {
    // Print maps — sirf relevant lines
try {
    java.io.BufferedReader br = new java.io.BufferedReader(
        new java.io.FileReader("/proc/self/maps"));
    String line;
    Log.i("MAPS", "=== START ===");
    while ((line = br.readLine()) != null) {
        if (line.contains("libbgmi") || line.contains("loader") || line.contains("libpubgm") || line.contains(".so")) {
            Log.i("MAPS", line);
        }
    }
    Log.i("MAPS", "=== END ===");
    br.close();
} catch (Exception e) {
    Log.e("MAPS", "Error: " + e.getMessage());
}
    }

    // ========== ANONYMOUS LOADER ==========

    private static Object unsafeObj;
    private static java.lang.reflect.Method putByteMethod;
    private static java.lang.reflect.Method allocateMemoryMethod;
    private static java.lang.reflect.Method pageSizeMethod;
    private static java.lang.reflect.Method freeMemoryMethod;
    private static java.lang.reflect.Method mprotectMethod;
    private static int PROT_READ = 1;
    private static int PROT_EXEC = 4;

    private static boolean initUnsafe() {
        if (unsafeObj != null) return true;
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            java.lang.reflect.Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafeObj = f.get(null);
            putByteMethod = unsafeClass.getMethod("putByte", long.class, byte.class);
            allocateMemoryMethod = unsafeClass.getMethod("allocateMemory", long.class);
            pageSizeMethod = unsafeClass.getMethod("pageSize");
            freeMemoryMethod = unsafeClass.getMethod("freeMemory", long.class);

            try {
                Class<?> osClass = Class.forName("android.system.Os");
                mprotectMethod = osClass.getMethod("mprotect", long.class, long.class, int.class);
                Class<?> osConst = Class.forName("android.system.OsConstants");
                PROT_READ = osConst.getDeclaredField("PROT_READ").getInt(null);
                PROT_EXEC = osConst.getDeclaredField("PROT_EXEC").getInt(null);
            } catch (Exception ignored) {}

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Unsafe init failed", e);
            return false;
        }
    }
    private static boolean loadAnonymously(File file) {
        try {
            byte[] elf = java.nio.file.Files.readAllBytes(file.toPath());
    
          // Create memfd
java.lang.reflect.Method memfdCreate = Class.forName("android.system.Os")
.getMethod("memfd_create", String.class, int.class);
int MFD_CLOEXEC = Class.forName("android.system.OsConstants")
.getDeclaredField("MFD_CLOEXEC").getInt(null);
Object fdObj = memfdCreate.invoke(null, "anonlib", MFD_CLOEXEC);

// Get int fd from FileDescriptor
java.lang.reflect.Field fdField = java.io.FileDescriptor.class.getDeclaredField("fd");
fdField.setAccessible(true);
int mfd = fdField.getInt(fdObj);

// Write ELF
Class.forName("android.system.Os")
.getMethod("write", int.class, byte[].class, int.class, int.class)
.invoke(null, mfd, elf, 0, elf.length);

// lseek
Class.forName("android.system.Os")
.getMethod("lseek", int.class, long.class, int.class)
.invoke(null, mfd, 0L, 0);

// Load
System.load("/proc/self/fd/" + mfd);

// Close
Class.forName("android.system.Os")
.getMethod("close", int.class)
.invoke(null, mfd);
            Log.i(TAG, "MEMFD loaded");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "MEMFD failed: " + e.getMessage());
            return false;
        }
    }
    // ========== NATIVE METHODS ==========

    public static native void init(int apiLevel);
    public static native void enableIO();
    public static native void addIORule(String targetPath, String relocatePath);
    public static native void hideXposed();
    public static native String MundoSdk();

    @Keep
    public static int getCallingUid(int origCallingUid) {
        if (origCallingUid > 0 && origCallingUid < Process.FIRST_APPLICATION_UID)
            return origCallingUid;
        if (origCallingUid > Process.LAST_APPLICATION_UID)
            return origCallingUid;
        if (origCallingUid == ZCoreCore.getHostUid()) {
            if(BActivityThread.getAppPackageName().equals("com.google.android.gms"))
                return Process.ROOT_UID;
            if(BActivityThread.getAppPackageName().equals("com.google.android.webview"))
                return Process.myUid();
            return BActivityThread.getCallingBUid();
        }
        return origCallingUid;
    }

    @Keep
    public static String redirectPath(String path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static File redirectPath(File path) {
        return IOCore.get().redirectPath(path);
    }
}