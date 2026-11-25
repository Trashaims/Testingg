#ifndef ANTIHOOK_H
#define ANTIHOOK_H

#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

// Main detection System make On Your Loader Libso
//This Is For Loaders And Mods
bool is_hook_detected();
bool is_hook_detected_ultimate();
bool check_for_global_hooks();
bool verify_function_signature(const char *func_name);
bool check_hook_pattern(unsigned char *ptr, size_t size);
bool check_memory_protection(void *addr);
void init_anti_hook();

#ifdef __cplusplus
}
#endif

#endif // ANTIHOOK_H
