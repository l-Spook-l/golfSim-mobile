@echo off
"C:\\Users\\mihai\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HS:\\Android projects\\GolfSimMobile\\OpenCV\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=21" ^
  "-DANDROID_PLATFORM=android-21" ^
  "-DANDROID_ABI=x86" ^
  "-DCMAKE_ANDROID_ARCH_ABI=x86" ^
  "-DANDROID_NDK=C:\\Users\\mihai\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125" ^
  "-DCMAKE_ANDROID_NDK=C:\\Users\\mihai\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\Users\\mihai\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=C:\\Users\\mihai\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=S:\\Android projects\\GolfSimMobile\\OpenCV\\build\\intermediates\\cxx\\Debug\\e5r5v2a3\\obj\\x86" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=S:\\Android projects\\GolfSimMobile\\OpenCV\\build\\intermediates\\cxx\\Debug\\e5r5v2a3\\obj\\x86" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BS:\\Android projects\\GolfSimMobile\\OpenCV\\.cxx\\Debug\\e5r5v2a3\\x86" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
