# fat-aar
[![Release](https://img.shields.io/github/release/cpdroid/fat-aar.svg)](https://github.com/cpdroid/fat-aar/releases)
[![License MIT](https://img.shields.io/github/license/cpdroid/fat-aar.svg)](https://github.com/cpdroid/fat-aar/blob/master/LICENSE)
[![Android Gradle Plugin 3.0.0-3.4.2](https://img.shields.io/badge/android--gradle-3.0.0--3.4.2-blue)](https://developer.android.com/studio/releases/gradle-plugin#updating-gradle)
[![Gradle 4.6-5.1.1](https://img.shields.io/badge/gradle-4.6--5.1.1-blue)](https://developer.android.com/studio/releases/gradle-plugin#updating-gradle)

[中文文档](https://github.com/cpdroid/fat-aar/blob/master/README_CN.md)

A plugin to merge dependencies(aar/jar) into aar file.

This plugin can embed dependency(local or remote) to aar file, when an app module includes the aar file, the aar's dependencies code and resource 
can be referenced directly, no need to import once again.

**This plugin support android gradle plugin 3.0.0-3.4.2 and gradle 4.6-5.1.1.**

## Usage
Add snippet below to your ***root project's build.gradle*** file:
```gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'com.cpdroid:fat-aar:1.1.0'
  }
}
```

Add snippet below to you ***library module's build.gradle*** file:
```gradle
apply plugin: 'com.android.library'
apply plugin: "com.cpdroid.fat-aar"
```

***make sure 'com.cpdroid.fat-aar' is appied after 'com.android.library'***

Then you can use keyword *"embedded"* instead of *"implementation"* or *"compile"* to package target dependency to your generated aar file:
```gradle
embedded fileTree(dir: 'libs', include: ['*.aar'])
embedded project(':moduleA')
embedded 'com.gongwen:marqueelibrary:1.1.3'
```

Run gradle assemble task, normally *"assembleRelease"*, copy the generated aar file to your app module's libs directory and include the generated aar file in ***app module's build.gradle*** file:
```gradle
implementation fileTree(dir: 'libs', include: ['*.aar'])
```
Then you can reference the aar and it's dependencies code and resources.

[An example can be found here](https://github.com/cpdroid/fat-aar-sample)

## Configuration
Add *fataar* closure in ***library module's build.gradle*** file：
```gradle
apply plugin: 'com.android.library'
apply plugin: 'com.cpdroid.fat-aar'

android {
    compileSdkVersion 28
    ...
}

fataar {
    verboseLog true
    ignoreAndroidSupport true
    ignoreDependencies 'com.google.code.gson:gson:2.8.2'
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    ...
}
```
*fataar* closure is optional, and is in the same level with *android* and *dependencies*. Below is the detailed configuration：
* ***verboseLog***: Default vaule is *false*, set to  *true* to show more log
* ***ignoreAndroidSupport***: Whether ignore android support package automatically, default is *true*, *false* means won't ignore android support package
* ***ignoreDependencies***: Determine which dependency won't be embedded into aar, we can use it as follows：
  + match the ***'group:name:version'*** pattern
  ```gradle
    ignoreDependencies 'com.google.code.gson:gson:2.8.2'
  ```
  + only match the ***'name'*** pattern(part between two colon)：
  ```gradle
    ignoreDependencies 'gson'
  ```
  + regex match：
  ```gradle
    ignoreDependencies '^com.android.*'
  ```
  + you can add more than one *ignoreDependencies*, and it accept more than one patten：
  ```gradle
    fataar {
        ignoreDependencies 'com.blankj:utilcode:1.23.7', 'gson'    //accept more than one pattern,split by ','
        ignoreDependencies '^com.android.*'                        //add the second ignoreDependencies
    }
  ```
Any dependency matched the three pattern above will be ignored, and won't be packaged into aar file.

Set ***verboseLog*** to *true* to see more log like this:
```bash
> Task :mylibrary:decompressFreeReleaseDependencies
   pattern:[com.google.code.gson:gson:2.8.2], artifact:[com.google.code.gson:gson:2.8.2]
   Ignore matched dependency: [com.google.code.gson:gson:2.8.2]
```
*pattern* is your configuration, *artifact* is the dependency, if it matched the log will show  
*'Ignore matched dependency: [com.google.code.gson:gson:2.8.2]'*

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
   Found value [app_name] in [D:\workspace\fat-aar-sample\moduleA\build\fat-aar\exploded_aar\com.gongwen\marqueelibrary\1.1.3\res\values\values.xml]
   Delete 1 values...
```