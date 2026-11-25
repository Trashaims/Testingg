#include "AntiHook.h"
#include <dlfcn.h>
#include <string.h>
#include <unistd.h>
#include <sys/mman.h>
#include <android/log.h>
#include <link.h>
#include <elf.h>
#include <stdlib.h>
#include <inttypes.h>
#include <stdio.h>

#define LOG_TAG "AntiHook"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// Architecture detection
#if defined(__arm__)
    #define IS_ARM 1
#elif defined(__aarch64__)
    #define IS_ARM64 1
#elif defined(__i386__) || defined(__x86_64__)
    #define IS_X86 1
#endif

// ============================================================
// This Antihook System is free to use, modify, and distribute for any lawful purpose. No copyright claims. No fees. No attribution required.
//Owner - @NgrLeader
// ============================================================
void init_anti_hook() {
    static const int log_result = LOGI("AntiHook By Lizard initialized");
    (void)log_result;
}



// ============================================================
// METHOD 1: Direct byte pattern check
// ============================================================
bool check_hook_pattern(unsigned char *ptr, size_t size) {
    if (!ptr) return false;

#ifdef IS_ARM
    // ARM32 detection
    if ((uintptr_t)ptr & 1) {
        uint16_t *thumb = (uint16_t *)((uintptr_t)ptr & ~1);
        if (thumb[0] == 0xF000 || thumb[0] == 0xF8DF) return true;
        if (thumb[0] == 0x4778) return true;
        if (thumb[0] == 0xE8BD) return true;
        if (thumb[0] == 0xBD10 || thumb[0] == 0xBD20) return true;
    } else {
        uint32_t *arm = (uint32_t *)ptr;
        if ((arm[0] & 0xFF000000) == 0xE5000000) return true;
        if ((arm[0] & 0x0F000000) == 0x0A000000) return true;
        if ((arm[0] & 0xFFFF0000) == 0xE59FF000) return true;
    }

#elif defined(IS_ARM64)
    uint32_t *code = (uint32_t *)ptr;
    if ((code[0] & 0x9F00001F) == 0x90000000) {
        if ((code[1] & 0xFFC00000) == 0xF9400000) return true;
    }
    if ((code[0] & 0xFFFFFC1F) == 0xD61F0200) return true;
    if ((code[0] & 0xFC000000) == 0x14000000) return true;
    if ((code[0] & 0xFC000000) == 0x94000000) return true;
    if ((code[0] & 0xFFFFFBFF) == 0xD65F0000) return true;

#elif defined(IS_X86)
    if (size < 16) return false;
    if (ptr[0] == 0xE9) return true;
    if (ptr[0] == 0xE8) return true;
    if (ptr[0] == 0xFF && ptr[1] == 0x25) return true;
    if (ptr[0] == 0x68 && ptr[5] == 0xC3) return true;
    if (ptr[0] == 0xB8 && ptr[5] == 0xE9) return true;
    if (ptr[0] == 0xC3 || ptr[0] == 0xC2) return true;
    if (ptr[0] == 0xCC) return true;
#endif

    return false;
}

// ============================================================
// METHOD 2: GOT/PLT verification
// ============================================================
struct dl_phdr_info_callback {
    const char *func_name;
    void *func_addr;
    bool found;
};

static int dl_callback(struct dl_phdr_info *info, size_t size, void *data) {
    struct dl_phdr_info_callback *cb = (struct dl_phdr_info_callback *)data;

    if (!info->dlpi_name || info->dlpi_name[0] == '\0') {
        return 0;
    }

    void *handle = dlopen(info->dlpi_name, RTLD_LAZY | RTLD_NOLOAD);
    if (handle) {
        void *addr = dlsym(handle, cb->func_name);
        if (addr && addr == cb->func_addr) {
            cb->found = true;
        }
        dlclose(handle);
    }

    return 0;
}

bool verify_function_origin(const char *func_name) {
    void *addr = dlsym(RTLD_DEFAULT, func_name);
    if (!addr) return false;

    struct dl_phdr_info_callback cb;
    cb.func_name = func_name;
    cb.func_addr = addr;
    cb.found = false;

    dl_iterate_phdr(dl_callback, &cb);
    return cb.found;
}

// ============================================================
// METHOD 3: Compare with libc
// ============================================================
void *get_libc_function(const char *name) {
    void *libc = dlopen("libc.so", RTLD_LAZY | RTLD_NOLOAD);

    if (libc) {
        void *func = dlsym(libc, name);
        dlclose(libc);
        return func;
    }
    return nullptr;
}

bool check_against_libc(const char *func_name) {
    void *current = dlsym(RTLD_DEFAULT, func_name);
    void *libc_func = get_libc_function(func_name);

    if (!current || !libc_func) return false;
    if (current != libc_func) return true;

    return check_hook_pattern((unsigned char *)current, 32);
}

// ============================================================
// METHOD 4: Memory protection check
// ============================================================
bool check_memory_protection(void *addr) {
    if (!addr) return false;

    long page_size = sysconf(_SC_PAGESIZE);
    uintptr_t page_addr = (uintptr_t)addr & ~(page_size - 1);

    FILE *fp = fopen("/proc/self/maps", "r");
    if (!fp) return false;

    char line[512];
    bool suspicious = false;

    while (fgets(line, sizeof(line), fp)) {
        uintptr_t start, end;
        char perms[5];

        if (sscanf(line, "%" SCNxPTR "-%" SCNxPTR " %4s", &start, &end, perms) == 3) {
            if (page_addr >= start && page_addr < end) {
                if (perms[0] == 'r' && perms[2] == 'x' && perms[1] == 'w') {
                    suspicious = true;
                }
                if (perms[0] == 'r' && perms[2] != 'x') {
                    suspicious = true;
                }
                break;
            }
        }
    }

    fclose(fp);
    return suspicious;
}

// ============================================================
// METHOD 5: Function signature verification
// ============================================================
bool verify_function_signature(const char *func_name) {
    void *addr = dlsym(RTLD_DEFAULT, func_name);
    if (!addr) return false;

    if (check_hook_pattern((unsigned char *)addr, 32)) {
        LOGD("Hook detected: Byte pattern mismatch in %s", func_name);
        return true;
    }

    if (!verify_function_origin(func_name)) {
        LOGD("Hook detected: Origin mismatch in %s", func_name);
        return true;
    }

    if (check_against_libc(func_name)) {
        LOGD("Hook detected: Libc address mismatch in %s", func_name);
        return true;
    }

    if (check_memory_protection(addr)) {
        LOGD("Hook detected: Suspicious memory protection in %s", func_name);
        return true;
    }

    return false;
}

// ============================================================
// GLOBAL HOOK DETECTION
// ============================================================
bool check_for_global_hooks() {
    // Check for Xposed
    if (getenv("XPOSED_MODULE_PATH") ||
        getenv("XPOSED_LOADED") ||
        getenv("XPOSED_BRIDGE")) {
        LOGD("Xposed framework detected");
        return true;
    }

    // Check for Frida
    if (getenv("FRIDA_AGENT") ||
        getenv("FRIDA_SERVER") ||
        getenv("FRIDA_DEVICE")) {
        LOGD("Frida framework detected");
        return true;
    }

    // Check for Substrate
    if (getenv("SUBSTRATE_LOADED")) {
        LOGD("Substrate framework detected");
        return true;
    }

    // Check for LD_PRELOAD
    const char *ld_preload = getenv("LD_PRELOAD");
    if (ld_preload && strlen(ld_preload) > 0) {
        LOGD("LD_PRELOAD hooking detected: %s", ld_preload);
        return true;
    }

    // Check for suspicious libraries
    FILE *fp = fopen("/proc/self/maps", "r");
    if (fp) {
        char line[512];
        int lib_count = 0;

        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, ".so")) lib_count++;

            if (strstr(line, "frida") || strstr(line, "xposed") ||
                strstr(line, "substrate") || strstr(line, "hook")) {
                fclose(fp);
                LOGD("Suspicious library found: %s", line);
                return true;
            }
        }
        fclose(fp);

        if (lib_count > 50) {
            LOGD("Too many libraries loaded: %d", lib_count);
            return true;
        }
    }

    return false;
}

// ============================================================
// MAIN DETECTION FUNCTIONS
// ============================================================
bool is_hook_detected() {
    const char *critical_functions[] = {
        "strlen", "strcmp", "strcpy", "strcat", "strstr", "strchr",
        "memcpy", "memcmp", "memset", "memmove",
        "dlopen", "dlsym", "dlclose", "dlerror",
        "malloc", "free", "calloc", "realloc",
        "fopen", "fclose", "fread", "fwrite",
        "open", "close", "read", "write",
        "pthread_create", "pthread_join",
        "eglSwapBuffers", "eglMakeCurrent"
    };

    int hook_count = 0;
    int checked = 0;

    for (size_t i = 0; i < sizeof(critical_functions)/sizeof(critical_functions[0]); i++) {
        const char *func = critical_functions[i];

        if (dlsym(RTLD_DEFAULT, func)) {
            checked++;
            if (verify_function_signature(func)) {
                hook_count++;
                LOGD(" HOOK DETECTED in %s", func);

                if (strcmp(func, "strlen") == 0 ||
                    strcmp(func, "dlsym") == 0 ||
                    strcmp(func, "dlopen") == 0) {
                    return true;
                }
            }
        }
    }

    if (checked > 0 && (float)hook_count / checked > 0.2) {
        LOGD(" Multiple hooks detected: %d/%d", hook_count, checked);
        return true;
    }

    return check_for_global_hooks();
}

bool is_hook_detected_ultimate() {
    init_anti_hook();

    if (is_hook_detected()) {
        LOGE("HOOK DETECTED by function signature verification");
        return true;
    }

    if (check_for_global_hooks()) {
        LOGE(" HOOK DETECTED by global detection");
        return true;
    }

    LOGI("No hooks detected");
    return false;
}

// Run once when the containing native library is loaded.
__attribute__((constructor)) static void run_anti_hook_on_load() {
    is_hook_detected_ultimate();
}

// ============================================================
// COMPATIBILITY WRAPPERS
// ============================================================
bool is_hook_detected_v2() { return verify_function_signature("strlen"); }
bool is_hook_detected_v3() { return verify_function_signature("eglSwapBuffers"); }
bool is_hook_detected_v4() { return verify_function_signature("memcpy"); }
bool is_hook_detected_v5() { return false; }
bool is_hook_detected_v6() { return false; }
bool is_hook_detected_v7() { return is_hook_detected_v4(); }
bool is_hook_detected_v8() { return verify_function_signature("memcmp"); }
bool is_hook_detected_v9() { return verify_function_signature("memset"); }
bool is_hook_detected_v10() { return verify_function_signature("dlopen"); }
bool is_hook_detected_v11() { return verify_function_signature("dlsym"); }
bool is_hook_detected_v12() { return verify_function_signature("malloc"); }
bool is_hook_detected_v13() { return is_hook_detected_v11(); }

// ============================================================
// TEST FUNCTION
// ============================================================
void test_anti_hook() {
    init_anti_hook();
    LOGI("=== Anti-Hook Detection Test ===");

    bool result = is_hook_detected_ultimate();

    if (result) {
        LOGE("!!! HOOK DETECTED !!!");
    } else {
        LOGI("System appears clean");
    }
}
