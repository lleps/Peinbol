cmake_minimum_required(VERSION 3.4.1)



add_library( # Sets the name of the library.
		native-lib

		# Sets the library as a shared library.
		SHARED

		# Provides a relative path to your source file(s).
		native-lib.cpp
        JNI_PhysicsImpl.cpp
        #"bullet/btBulletCollisionAll.cpp"
        #"bullet/btBulletDynamicsAll.cpp"
        #"bullet/btLinearMathAll.cpp"
        )

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
