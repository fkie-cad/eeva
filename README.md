# E<sup>2</sup>VA, the *Damn Vulnerable App*

E<sup>2</sup>VA is an app allowing for binary exploitation on Android OS. It allows an external user to select and communicate with modules that contain severe vulnerabilities. Therefore, E<sup>2</sup>VA enables research on the applicability of *standard* binary exploitation techniques to Android apps, which call native functions.

E<sup>2</sup>VA stands for *Exploitation Experience (with) Vulnerable App*. It is the foundation of a series of [blog posts](https://lolcads.github.io/posts/2022/11/diving_into_the_art_of_userspace_exploitation_under_android/) that describe exploitation of some already existing vulnerable modules. The app runs on a Pixel 3 emulator (without Google Play for root access), running Android 12 (API level 31), and an x86 - 64 architecture. Other setups have not yet been tested!

## Installation

**Warning**: This app requests a [runtime - permission](https://developer.android.com/reference/android/provider/Settings.html#ACTION_MANAGE_OVERLAY_PERMISSION) that it needs to stay active for e.g. long debugging sessions. As this app contains **actual vulnerabilities**, there is always a risk of some third party attacking the app, getting code execution and rampaging through your system. Therefore, launch E<sup>2</sup>VA in a controlled environment, which does not contain any important and/or personal information that must not be lost and/or leaked. (or accept the risk)

Currently, there are two ways of installing E<sup>2</sup>VA.

### APK

Use the `.apk` file, which is (hopefully kept) up to date with the current version of E<sup>2</sup>VA. This is the *intended* route, i.e. taking perspective of an attacker, it is more likely to have access to an `.apk` - file than the app's original source code.

### Build via *Android Studio*

As this is just the pushed [*Android Studio*](https://developer.android.com/studio) project of E<sup>2</sup>VA, one can just build the app, create own modules, optimize communication between external client and E<sup>2</sup>VA etc.

## Emulator Hardware Profile

Up to this point, an AVD (Android Virtual Device) to run E<sup>2</sup>VA can be created by either using a predefined hardware profile in Android Studio (called *Pixel 3*), or by importing the hardware profile in this repository.
