package com.cpdroid.fat_aar

import com.android.ide.common.symbols.*
import com.google.common.base.Charsets
import com.google.common.io.Files
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.FileTree
import java.lang.reflect.Field
import static Utils.*

class FatAarPlugin implements Plugin<Project> {

    private List<File> embeddedJars = new ArrayList()
    private List<String> embeddedAarDirs = new ArrayList<>()

    private String build_dir
    private String exploded_aar_dir
    private String packaged_class
    private String intermediates_dir
    private String temp_classes_dir

    private String mAndroidGradleVersion

    private void initVariables(Project project) {
        build_dir = project.buildDir.path.replace(File.separator, '/')
        packaged_class = "$build_dir/intermediates/packaged-classes"
        intermediates_dir = "$build_dir/intermediates"
        temp_classes_dir = "$build_dir/aar_plugin/temp-classes"
        exploded_aar_dir = "$build_dir/aar_plugin/exploded_aar"

        project.parent.buildscript.configurations.classpath.dependencies.each {
            if (it.group == "com.android.tools.build" && it.name == "gradle")
                mAndroidGradleVersion = it.version
        }
    }

    @Override
    void apply(Project project) {
        initVariables(project)

        project.configurations {
            embedded
        }

        project.dependencies {
            implementation project.configurations.embedded
        }

        project.afterEvaluate {
            logLevel1 "plugin embed size:" + project.configurations.embedded.resolve().size()

            if (project.configurations.embedded.resolve().size() <= 0) return

            createDecompressTask(project)

            project.android.libraryVariants.all {
                libraryVariant ->
                    String flavorName = libraryVariant.flavorName
                    String buildType = libraryVariant.buildType.name
                    String flavorBuildType = libraryVariant.name.capitalize()
                    boolean enableProguard = libraryVariant.buildType.minifyEnabled

                    Task addSourceSetsTask = project.task("add${flavorBuildType}SourceSets").doLast {
                        embeddedAarDirs.each {
                            deleteAppNameAttribute(it)
                            project.android.sourceSets.main.res.srcDirs += project.file("$it/res")
                            project.android.sourceSets.main.aidl.srcDirs += project.file("$it/aidl")
                            project.android.sourceSets.main.assets.srcDirs += project.file("$it/assets")
                            project.android.sourceSets.main.jniLibs.srcDirs += project.file("$it/jni")
                        }
                    }

                    addSourceSetsTask.dependsOn(project.tasks.decompressDependencies)
                    project.tasks."pre${flavorBuildType}Build".dependsOn(addSourceSetsTask)

                    Task embedManifestsTask = project.task("embed${flavorBuildType}Manifests").doLast {
                        embedManifests(flavorBuildType)
                    }
                    embedManifestsTask.dependsOn(project.tasks."process${flavorBuildType}Manifest")

                    //Generated R.jar file and merge library jar
                    Task embedJarTask = project.task("embed${flavorBuildType}LibJarAndRClass").doLast {
                        generateRJar(flavorName, buildType)

                        if (!enableProguard) {
                            embeddedAarDirs.each { aarPath ->
                                logLevel2 "lib dir:" + aarPath

                                // Copy all additional jar files to bundle lib
                                FileTree jars = project.fileTree(dir: aarPath, include: '*.jar', exclude: 'classes.jar')
                                jars += project.fileTree(dir: "$aarPath/libs", include: '*.jar')
                                embeddedJars.addAll(jars)
                                logLevel2 "jars:" + jars.getFiles()
                            }
                            embeddedJars.addAll(project.fileTree(dir: temp_classes_dir))

                            project.copy {
                                from embeddedJars
                                into project.file("$packaged_class/$flavorName/$buildType/libs")
                            }
                        }
                    }
                    embedJarTask.dependsOn(project.tasks."transformClassesAndResourcesWithSyncLibJarsFor${flavorBuildType}")

                    Task bundleAarTask = project.tasks."bundle${flavorBuildType}Aar"
                    bundleAarTask.dependsOn embedManifestsTask
                    bundleAarTask.dependsOn embedJarTask
            }
        }
    }

    private void createDecompressTask(Project project) {
        //Decompress all dependencies to build/exploded_aar directory
        project.task("decompressDependencies").doLast {
            List<ResolvedArtifact> artifactList = new ArrayList<>()

            //Add local dependencies
            project.configurations.embedded.files { it.name == "unspecified" }.each {
                artifactList.add(new LocalResolvedArtifact(it))
            }

            //Add remote dependencies
            artifactList.addAll(project.configurations.embedded.resolvedConfiguration.resolvedArtifacts)

            List<String> embeddedPackage = new ArrayList<>()

            artifactList.each {
                artifact ->
                    logLevel1 "artifact: " + artifact

                    String destination = ""
                    String rename_classes_jar = ""

                    if (artifact instanceof LocalResolvedArtifact) {
                        destination = exploded_aar_dir + "/localDependencies/" + Files.getNameWithoutExtension(artifact.name)
                        rename_classes_jar = Files.getNameWithoutExtension(artifact.name)
                    } else {
                        destination = exploded_aar_dir + "/" + artifact.moduleVersion.toString().replace(":", "/")
                        rename_classes_jar = artifact.name
                    }

                    if (artifact.type == 'aar') {
                        //Decompress aar file
                        if (artifact.file.isFile()) {
                            logLevel2 "from:     " + artifact.file
                            logLevel2 "unzip to: " + destination
                            project.copy {
                                from project.zipTree(artifact.file)
                                into destination
                            }
                        }

                        String pkgName = new XmlParser().parse("$destination/AndroidManifest.xml").@package

                        //Ignore android support package and duplicate package
                        if (pkgName.startsWith("android.") || embeddedPackage.contains(pkgName)) return

                        embeddedPackage.add(pkgName)
                        if (!embeddedAarDirs.contains(destination)) embeddedAarDirs.add(destination)

                        project.copy {
                            from "$destination/classes.jar"
                            into "$temp_classes_dir/"
                            rename "classes.jar", "${rename_classes_jar}.jar"
                        }

                    } else if (artifact.type == 'jar') {
                        logLevel2 "jar info: " + artifact.name + " " + artifact.id + " " + artifact.classifier + " " + artifact.moduleVersion.id.group
                        String groupName = artifact.moduleVersion.id.group + ":" + artifact.name

                        //Ignore android jar
                        if (groupName.startsWith("android.") || groupName.startsWith("com.android.")
                                || embeddedPackage.contains(groupName)) return

                        embeddedPackage.add(groupName)
                        if (!embeddedJars.contains(artifact.file)) embeddedJars.add(artifact.file)

                    } else {
                        throw new Exception("Unhandled Artifact of type ${artifact.type}")
                    }
            }

            reportEmbeddedFiles()
        }
    }

    private void reportEmbeddedFiles() {
        StringBuilder builder = new StringBuilder()

        builder.append("\n-- embedded aar dirs --\n")
        embeddedAarDirs.each {
            builder.append(it)
            builder.append("\n")
        }

        builder.append("\n-- embedded jars --\n")
        embeddedJars.each {
            builder.append(it)
            builder.append("\n")
        }

        Files.asCharSink(new File("$build_dir/embedDependencies.txt"), Charsets.UTF_8).write(builder.toString())
    }

    private void embedManifests(String flavorBuildType) {
        File mainManifest

        if (mAndroidGradleVersion != null
                && compareAndroidGradleVersion(mAndroidGradleVersion, "3.3.0") >= 0) {
            mainManifest = new File("$intermediates_dir/library_manifest/$flavorBuildType/AndroidManifest.xml")
        } else {
            mainManifest = new File("$intermediates_dir/merged_manifests//$flavorBuildType/process${flavorBuildType}Manifest/merged/AndroidManifest.xml")
        }

        if (mainManifest == null || !mainManifest.exists()) return

        List<File> libraryManifests = new ArrayList<>()
        embeddedAarDirs.each {
            File manifest = new File("$it/AndroidManifest.xml")
            if (!libraryManifests.contains(manifest) && manifest.exists()) {
                libraryManifests.add(manifest)
            }
        }

        File reportFile = new File("${build_dir}/embedManifestReport.txt")
        mergeManifest(mainManifest, libraryManifests, reportFile)
    }

    private void generateRJar(String flavorName, String buildType) {
        List<SymbolTable> tableList = new ArrayList<>()

        embeddedAarDirs.each {
            aarDir ->
                logLevel1 "aar directory:" + aarDir

                String aarPackageName = new XmlParser().parse("${aarDir}/AndroidManifest.xml").@package
                logLevel2 "aarPackage: " + aarPackageName

                File resDir = new File(aarDir + "/res")
                if (resDir.listFiles() == null) return

                SymbolTable table = ResourceDirectoryParser.parseResourceSourceSetDirectory(
                        resDir, IdProvider.@Companion.sequential(), null)

                Field field = table.getClass().getDeclaredField("tablePackage")
                field.setAccessible(true)
                field.set(table, aarPackageName)

                tableList.add(table)
        }

        String currentPackageName = new XmlParser().parse(new File(build_dir).parent + "/src/main/AndroidManifest.xml").@package
        logLevel1 "targetPackageName: " + currentPackageName
        exportToCompiledJava(tableList, currentPackageName, new File("$packaged_class/$flavorName/$buildType/libs/R.jar").toPath())
    }
}