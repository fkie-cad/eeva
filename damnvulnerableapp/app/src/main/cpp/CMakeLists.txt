# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.

project("damnvulnerableapp")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        StackBufferOverflowModule

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        modules/StackBufferOverflowModule.c)

add_library(
        EasyStackBufferOverflowModule

        SHARED

        modules/EasyStackBufferOverflowModule.c
)

add_library(
        UseAfterFreeWriteModule

        SHARED

        modules/UseAfterFreeWriteModule.c
)

add_library(
        UseAfterFreeExecModule

        SHARED

        modules/UseAfterFreeExecModule.c
)

add_library(
        DoubleFreeModule

        SHARED

        modules/DoubleFreeModule.c
)

add_library(
        HeapOverflowModule

        SHARED

        modules/HeapOverflowModule.c
)

add_library(
        OffByOneModule

        SHARED

        modules/OffByOneModule.c
)

add_library(
        SecondaryFakeModule

        SHARED

        modules/SecondaryFakeModule.c
)

add_library(
        HeapSCAModule

        SHARED

        modules/HeapSCAModule.c
)

add_library(
        PoCPrimaryPoisoning

        SHARED

        modules/PoCPrimaryPoisoning.c
)