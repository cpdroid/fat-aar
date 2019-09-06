# fat-aar
[![Release](https://img.shields.io/github/release/cpdroid/fat-aar.svg)](https://github.com/cpdroid/fat-aar/releases)
[![License MIT](https://img.shields.io/github/license/cpdroid/fat-aar.svg)](https://github.com/cpdroid/fat-aar/blob/master/LICENSE)
[![Android Gradle Plugin 3.0.0-3.4.2](https://img.shields.io/badge/android--gradle-3.0.0--3.4.2-blue)](https://developer.android.com/studio/releases/gradle-plugin#updating-gradle)
[![Gradle 4.6-5.1.1](https://img.shields.io/badge/gradle-4.6--5.1.1-blue)](https://developer.android.com/studio/releases/gradle-plugin#updating-gradle)

Fat-aar是一个能将依赖库 *(aar/jar)* 打包进 *aar* 文件的插件.

它能将远程或本地依赖打包进 *aar* 文件，当 *app* 模块使用这个 *aar* 文件时，*aar* 依赖的代码跟资源可以直接被引用到，而无需在 *app* 模块里面重新引入一遍.

本插件支持 *Android gradle plugin* 3.0.0-3.4.2 及 *Gradle* 4.6-5.1.1 版本.

## 用法
在**根目录的 *build.gradle*** 里面添加如下语句:
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

在 ***library* 模块的 *build.gradle*** 里面添加如下语句:
```gradle
apply plugin: 'com.android.library'
apply plugin: "com.cpdroid.fat-aar"
```

**确保 'com.cpdroid.fat-aar' 在'com.android.library'后面**

用关键字 *"embedded"* 替代 *"implementation"* 或者 *"compile"* 就可以把依赖库打包进生成的 *aar* 文件：
```gradle
embedded fileTree(dir: 'libs', include: ['*.aar'])
embedded project(':moduleA')
embedded 'com.gongwen:marqueelibrary:1.1.3'
```

运行 *gradle* 任务, 一般来说是 *"assembleRelease"*, 把生成的 *aar* 文件复制进 *app* 模块的 *libs* 目录，然后在***app* 模块的 *build.gradle*** 里面引入生成的 *aar* 文件:
```gradle
implementation fileTree(dir: 'libs', include: ['*.aar'])
```
之后就可以在 *app* 模块里面使用 *aar* 及其依赖库的代码与资源了.

[这有个例子](https://github.com/cpdroid/fat-aar-sample)

## 配置
在 ***library* 模块的 *build.gradle*** 里添加 *fataar* 闭包可以进行自定义配置：
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
注意 *fataar* 配置是可选的，不配置插件也可以正常运行， *fataar* 与 *android* 和 *dependencies* 是平级的，各项配置说明如下：
* ***verboseLog***  默认为 *false* ，改为 *true* 可以显示更多日志
* ***ignoreAndroidSupport*** 是否自动忽略 *android support* 包，默认为 *true*，改为 *false* 则不会自动忽略 *android support* 包
* ***ignoreDependencies*** 配置哪些依赖不用打包进 *aar*，用法如下：
  + 完全匹配 *'group:name:version'* 的格式:
  ```gradle
    ignoreDependencies 'com.google.code.gson:gson:2.8.2'
  ```
  + 只匹配 *'name'* 部分(两个冒号中间的部分)：
  ```gradle
    ignoreDependencies 'gson'
  ```
  + 正则匹配：
  ```gradle
    ignoreDependencies '^com.android.*'
  ```
  + 可以同时添加多个 *ignoreDependencies* 配置，也可以在 *ignoreDependencies* 后面添加多个参数：
  ```gradle
    fataar {
        ignoreDependencies 'com.blankj:utilcode:1.23.7', 'gson'    //接收多个参数，以','分隔
        ignoreDependencies '^com.android.*'                        //添加第二条配置
    }
  ```
*ignoreDependencies* 的配置是叠加的不是覆盖的，像上面的配置只要依赖库匹配到三条中的任意一条都不会被打包进 *aar*.

如果想看具体匹配到哪些依赖，可以将 *verboseLog* 设置为 *true*，运行时可以看到如下 *log*:
```bash
> Task :mylibrary:decompressFreeReleaseDependencies
   pattern:[com.google.code.gson:gson:2.8.2], artifact:[com.google.code.gson:gson:2.8.2]
   Ignore matched dependency: [com.google.code.gson:gson:2.8.2]
```
*pattern* 是配置的规则，*artifact* 是正在检查的依赖库，如果匹配成功，就会提示  
*'Ignore matched dependency: [com.google.code.gson:gson:2.8.2]'*

## 特性或问题
* 本插件会自动忽略 *android support* 包和 *android* 的 *jar* 包，在 *app* 模块使用的时候必须重新导入，
可以将 *ignoreAndroidSupport* 设置为 *false* 以禁止自动忽略 *android support* 包和 *android* 的 *jar* 包.  
具体有哪些被忽略了可以看日志：
```bash
> Task :moduleA:decompressReleaseDependencies
   Ignore android package: [android.support.v7.appcompat]
   Ignore android package: [android.support.fragment]
   Ignore android jar: [com.android.support:support-annotations]
   Ignore android package: [android.arch.lifecycle]
   Ignore android jar: [android.arch.lifecycle:common]
   Ignore android jar: [android.arch.core:common]
```

* 依赖库的资源里的 *"app_name"* 字段会被自动删除，因为很多依赖库都有这一字段，编译时会冲突导致编译失败，
如果想用这个字段请在 *library* 的 *values.xml* 里重新定义一个。下面的日志显示删除了 *"app_name"* 的文件：
```bash
> Task :moduleA:addReleaseSourceSets
   Found value [app_name] in [D:\workspace\fat-aar-sample\moduleA\build\fat-aar\exploded_aar\com.gongwen\marqueelibrary\1.1.3\res\values\values.xml]
   Delete 1 values...
```
