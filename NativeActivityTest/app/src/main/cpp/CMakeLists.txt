cmake_minimum_required(VERSION 3.4.1)

# jni extension library
add_library(
        native-lib
		SHARED
		native-lib.cpp
        JNI_PhysicsImpl.cpp)

# Add bullet physics dependency
add_library(Bullet
        SHARED
        IMPORTED)
set_target_properties(
        Bullet
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libBullet.so)
include_directories(include/bullet)

# Search log lib
find_library(
		log-lib
		log)

# Link to dependencies
target_link_libraries(
        native-lib
		android
        Bullet
        log
		dl
		EGL
		GLESv2)
