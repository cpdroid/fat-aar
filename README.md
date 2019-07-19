# fat-aar

[![License MIT](https://img.shields.io/github/release-pre/cpdroid/fat-aar.svg)](https://github.com/cpdroid/fat-aar/releases/tag/v0.9.1)
[![Release](https://img.shields.io/github/license/cpdroid/fat-aar.svg)](https://github.com/cpdroid/fat-aar/blob/master/LICENSE)

A plugin to merge dependencies(aar/jar) into aar file

## Usage

Add snippet below to your root project's *build.gradle*:

```gradle
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.com.cpdroid.fat_aar:buildSrc:0.9.1"
  }
}
```

Add snippet below to you library project:

```gradle
apply plugin: 'com.android.library'
apply plugin: "com.cpdroid.fat_aar"
```

**make sure 'com.cpdroid.fat-aar' is appied after 'com.android.library'**
