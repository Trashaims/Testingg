cmake_minimum_required(VERSION 3.12)
project(AntiHook LANGUAGES CXX)

# Include the object in the consuming native library so its load callback is retained.
add_library(antihook OBJECT AntiHook.cpp)
set_target_properties(antihook PROPERTIES POSITION_INDEPENDENT_CODE ON)
target_compile_features(antihook PRIVATE cxx_std_11)
target_include_directories(antihook PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}")
target_link_libraries(antihook PUBLIC dl log)
