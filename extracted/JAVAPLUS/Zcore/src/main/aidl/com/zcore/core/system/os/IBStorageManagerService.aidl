package com.zcore.core.system.os;

import android.net.Uri;
import android.os.storage.StorageVolume;

interface IBStorageManagerService {
    StorageVolume[] getVolumeList(int uid, String packageName, int flags, int userId);
    Uri getUriForFile(String file);
}
