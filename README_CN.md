# fat-aar
[![License MIT](https://img.shields.io/github/release-pre/cpdroid/fat-aar.svg)](https://github.com/cpdroid/fat-aar/releases/tag/v0.9.1)
[![Release](https://img.shields.io/github/license/cpdroid/fat-aar.svg)](https://github.com/cpdroid/fat-aar/blob/master/LICENSE)

Fat-aar是一个将依赖库 *(aar/jar)* 打包进 *aar* 文件的插件.

它能将远程或者本地依赖嵌入 *aar* 文件，当 *app* 模块使用这个 *aar* 文件时，*aar* 依赖的代码跟资源可以直接被引用到，而无需在 *app* 模块里面重新引入一遍.

## 用法
在根目录的 *build.gradle* 文件里面添加如下语句:
```gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'com.cpdroid:fat-aar:0.9.4'
  }
}
```

在 *library* 工程的 *build.gradle* 里面添加如下语句:
```gradle
apply plugin: 'com.android.library'
apply plugin: "com.cpdroid.fat_aar"
```

**确保 'com.cpdroid.fat-aar' 在'com.android.library'后面**

用关键字 *"embedded"* 替代 *"implementation"* 或者 *"compile"* 就可以把依赖的库打包进你的 *aar* 文件：
```gradle
embedded fileTree(dir: 'libs', include: ['*.aar'])
embedded project(':moduleA')
embedded 'com.gongwen:marqueelibrary:1.1.3'
```

运行 *gradle* 任务, 一般来说是 *"assembleRelease"*, 把生成的 *aar* 文件复制进 *app* 模块的 *libs* 目录

## 特性或问题
* 本插件会自动忽略 *android support* 包和 *android* 开头的 *jar* 包，在 *app* 模块使用的时候必须重新导入，
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

* 依赖库的资源里的 *"app_name"* 字段会被自动删除，因为很多依赖库都有这一字段，编译时会冲突导致编译失败。
如果你想用这个字段请在 *library* 的 *values.xml* 里重新定义一个
```bash
> Task :moduleA:addReleaseSourceSets
   Found value [app_name] in [D:\workspace\fat-aar-sample\moduleA\build\aar_plugin\exploded_aar\com.gongwen\marqueelibrary\1.1.3\res\values\values.xml]
   Delete 1 values...
```


