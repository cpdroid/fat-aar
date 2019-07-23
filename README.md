# fat-aar

[![License MIT](https://img.shields.io/github/release-pre/cpdroid/fat-aar.svg)](https://github.com/cpdroid/fat-aar/releases/tag/v0.9.1)
[![Release](https://img.shields.io/github/license/cpdroid/fat-aar.svg)](https://github.com/cpdroid/fat-aar/blob/master/LICENSE)

A plugin to merge dependencies(aar/jar) into aar file

## Usage

Add snippet below to your root project's *build.gradle* file:

```gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'com.cpdroid:fat-aar:0.9.3'
  }
}
```

Add snippet below to you library project's *build.gradle* file:

```gradle
apply plugin: 'com.android.library'
apply plugin: "com.cpdroid.fat_aar"
```

**make sure 'com.cpdroid.fat-aar' is appied after 'com.android.library'**

Then you can use keyword *"embedded"* instead of *"implementation"* or *"compile"* to package target dependency to your generated aar file:

```gradle
embedded fileTree(dir: 'libs', include: ['*.aar'])
embedded project(':moduleA')
embedded 'com.gongwen:marqueelibrary:1.1.3'
```
