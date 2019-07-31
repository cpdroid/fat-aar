# fat-aar
[![Release](https://img.shields.io/github/release/cpdroid/fat-aar.svg)](https://github.com/cpdroid/fat-aar/releases)
[![License MIT](https://img.shields.io/github/license/cpdroid/fat-aar.svg)](https://github.com/cpdroid/fat-aar/blob/master/LICENSE)
[![Android Gradle Plugin 3.0.0-3.4.2](https://img.shields.io/badge/android--gradle-3.0.0--3.4.2-blue)](https://developer.android.com/studio/releases/gradle-plugin#updating-gradle)
[![Gradle 4.6-5.1.1](https://img.shields.io/badge/gradle-4.6--5.1.1-blue)](https://developer.android.com/studio/releases/gradle-plugin#updating-gradle)

[中文文档](https://github.com/cpdroid/fat-aar/blob/master/README_CN.md)

A plugin to merge dependencies(aar/jar) into aar file.

This plugin can embed dependency(local or remote) to an aar file, when an app module includes the aar file, the aar's dependencies code and resource 
can be referenced directly, no need to import once again.

**This plugin has been tested on android gradle plugin 3.0.0-3.4.2 and gradle 4.6-5.1.1.**

## Usage
Add snippet below to your **root project's *build.gradle*** file:
```gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'com.cpdroid:fat-aar:1.0.0'
  }
}
```

Add snippet below to you **library module's *build.gradle*** file:
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

Run gradle assemble task, normally *"assembleRelease"*, copy the generated aar file to your app module's libs directory

Include the generated aar file in **app module's *build.gradle*** file:
```gradle
implementation fileTree(dir: 'libs', include: ['*.aar'])
```

[An example can be found here](https://github.com/cpdroid/fat-aar-sample)

## Features or Issues
* All android support packages and android jars will be ignored automatically, you must import them in your app module.
The detailed ignored package can be found in the log.
```bash
> Task :moduleA:decompressReleaseDependencies
   Ignore android package: [android.support.v7.appcompat]
   Ignore android package: [android.support.fragment]
   Ignore android jar: [com.android.support:support-annotations]
   Ignore android package: [android.arch.lifecycle]
   Ignore android jar: [android.arch.lifecycle:common]
   Ignore android jar: [android.arch.core:common]
```

* All *"app_name"* attributes in embedded modules will be deleted automatically as a few modules has the attributes, and it will conflict while compiling.
If you have referenced a module's *app_name*, define an *app_name* in your library's *values.xml* file.
```bash
> Task :moduleA:addReleaseSourceSets
   Found value [app_name] in [D:\workspace\fat-aar-sample\moduleA\build\aar_plugin\exploded_aar\com.gongwen\marqueelibrary\1.1.3\res\values\values.xml]
   Delete 1 values...
```