//
// JNI hooks for java/lang/VMClassLoader.findLoadedClass
//

#ifndef VIRTUAL_APP_VMCLASSLOADERHOOK_H
#define VIRTUAL_APP_VMCLASSLOADERHOOK_H

#include <jni.h>

class VMClassLoaderHook {
public:
    static void init(JNIEnv *env);
    static void hideXposed();
};

#endif
