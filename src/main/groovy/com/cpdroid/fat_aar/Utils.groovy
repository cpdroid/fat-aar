package com.cpdroid.fat_aar

import com.android.SdkConstants
import com.android.build.gradle.internal.LoggerWrapper
import com.android.ide.common.symbols.*
import com.android.manifmerger.*
import com.android.manifmerger.ManifestMerger2.Invoker
import com.android.manifmerger.ManifestMerger2.MergeType
import com.android.resources.*
import com.android.utils.ILogger

import com.google.common.base.*
import com.google.common.io.Files

import groovy.transform.CompileStatic
import groovy.xml.XmlUtil

import org.gradle.api.Task
import org.gradle.api.logging.Logging

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import static org.objectweb.asm.Opcodes.*

import java.nio.file.Path
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

@CompileStatic
class Utils {

    static def logLevel1(Object value) {
        println ">> " + value
    }

    static def logLevel2(Object value) {
        println "   " + value
    }

    /**
     * Delete app_name attribute from values.xml
     * @param aarPath The root directory
     */
    static void deleteAppNameAttribute(String aarPath) {
        File[] files = new File(aarPath + "/res").listFiles()
        if (files == null) return

        for (File resourceDirectory : files) {
            if (!resourceDirectory.isDirectory()) {
                throw new ResourceDirectoryParseException(
                        resourceDirectory.getAbsolutePath() + " is not a directory")
            }

            assert (resourceDirectory.isDirectory())

            ResourceFolderType folderResourceType = ResourceFolderType.getFolderType(resourceDirectory.getName())
            if (folderResourceType != ResourceFolderType.VALUES) continue

            // Iterate all files in the resource directory and handle each one.
            File[] listFiles = resourceDirectory.listFiles()
            if (listFiles == null) continue

            for (File maybeResourceFile : listFiles) {
                if (maybeResourceFile.isDirectory()) continue

                if (!maybeResourceFile.isFile()) {
                    throw new ResourceDirectoryParseException(
                            "${maybeResourceFile.absolutePath} is not a file nor directory")
                }

                Node node = new XmlParser().parse(maybeResourceFile)
                NodeList string_node = (NodeList) node.get("string")

                int removeCount = 0

                if (string_node) {
                    string_node.each {
                        Node childNode = (Node) it
                        if (childNode.attribute("name") == "app_name") {
                            logLevel2 "Found value [app_name] in [" + maybeResourceFile.getAbsolutePath() + "]"
                            node.remove(childNode)
                            removeCount++
                        }
                    }
                }

                if (removeCount > 0) {
                    logLevel2 "Delete " + removeCount + " values..."
                    Files.asCharSink(maybeResourceFile, Charsets.UTF_8).write(XmlUtil.serialize(node))
                }
            }
        }
    }

    static int compareAndroidGradleVersion(String v1, String v2) {
        if (v1.isEmpty() || v2.isEmpty()) throw new Exception("Unable to compare empty version!")

        String[] str1 = v1.split("\\.")
        String[] str2 = v2.split("\\.")
        int minLength = str1.length <= str2.length ? str1.length : str2.length

        for (int index = 0; index < minLength; index++) {
            String s1 = str1[index]
            String s2 = str2[index]
            if (s1 != s2) return Long.valueOf(s1) > Long.valueOf(s2) ? 1 : -1
        }

        if (str1.length > minLength) {
            if (Long.valueOf(str1[minLength]) < 0)
                throw new Exception("Version [$v1] may be incorrect")
            return 1
        }

        if (str2.length > minLength) {
            if (Long.valueOf(str2[minLength]) < 0)
                throw new Exception("Version [$v2] may be incorrect")
            return -1
        }

        return 0
    }

    /**
     * Merge libraryManifests to mainManifest
     *
     * @param mainManifestFile
     * @param libraryManifests
     * @param reportFile
     */
    static void mergeManifest(File mainManifestFile, List<File> libraryManifests, File reportFile) {
        try {
            ILogger mLogger = new LoggerWrapper(Logging.getLogger(Task.class))

            Invoker manifestMergerInvoker = ManifestMerger2.newMerger(mainManifestFile, mLogger, MergeType.APPLICATION)
            manifestMergerInvoker.addLibraryManifests(libraryManifests.toArray(new File[libraryManifests.size()]))
            manifestMergerInvoker.setMergeReportFile(reportFile)

            MergingReport mergingReport = manifestMergerInvoker.merge()
            MergingReport.Result result = mergingReport.getResult()

            switch (result) {
                case MergingReport.Result.WARNING:
                    mergingReport.log(mLogger)
                    break

                case MergingReport.Result.SUCCESS:
                    XmlDocument xmlDocument = mergingReport.getMergedXmlDocument(MergingReport.MergedManifestKind.MERGED)
                    Files.asCharSink(mainManifestFile, Charsets.UTF_8).write(xmlDocument.prettyPrint())
                    break

                case MergingReport.Result.ERROR:
                    mergingReport.log(mLogger)
                    throw new RuntimeException(mergingReport.getReportString())

                default:
                    throw new RuntimeException("Unhandled result type : " + mergingReport.getResult())
            }
        } catch (Exception e) {
            e.printStackTrace()
            throw new RuntimeException(e)
        }
    }

    /**
     * Generate R.jar file
     * @param tableList Symbols of android resource
     * @param targetPackageName Current package name
     * @param outJar Destination of R.jar
     */
    static void exportToCompiledJava(List<SymbolTable> tableList, String targetPackageName, Path outJar) {
        JarOutputStream jarOutputStream = new JarOutputStream(
                new BufferedOutputStream(java.nio.file.Files.newOutputStream(outJar)))

        tableList.each {
            table ->
                String packageName = table.tablePackage
                EnumSet<ResourceType> resourceTypes = EnumSet.noneOf(ResourceType.class)
                for (resType in ResourceType.values()) {
                    // Don't write empty R$ classes.
                    byte[] bytes = generateResourceTypeClass(table, packageName, targetPackageName, resType)
                    if (bytes == null) continue

                    resourceTypes.add(resType)
                    String innerR = generateInternalName(packageName, resType)
                    jarOutputStream.putNextEntry(new ZipEntry(innerR + SdkConstants.DOT_CLASS))
                    jarOutputStream.write(bytes)
                }

                // Generate and write the main R class file.
                String packageR = generateInternalName(packageName, null)
                jarOutputStream.putNextEntry(new ZipEntry(packageR + SdkConstants.DOT_CLASS))
                jarOutputStream.write(generateOuterRClass(resourceTypes, packageR))
        }

        if (jarOutputStream != null) {
            jarOutputStream.flush()
            jarOutputStream.close()
        }
    }

    static byte[] generateOuterRClass(EnumSet<ResourceType> resourceTypes, String packageR) {
        ClassWriter cw = new ClassWriter(COMPUTE_MAXS)
        cw.visit(
                V1_8,
                ACC_PUBLIC + ACC_FINAL + ACC_SUPER,
                packageR, null,
                "java/lang/Object", null)

        for (rt in resourceTypes) {
            cw.visitInnerClass(
                    packageR + "\$" + rt.getName(),
                    packageR,
                    rt.getName(),
                    ACC_PUBLIC + ACC_FINAL + ACC_STATIC)
        }

        // Constructor
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null)
        mv.visitCode()
        mv.visitVarInsn(ALOAD, 0)
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        mv.visitInsn(RETURN)
        mv.visitMaxs(0, 0)
        mv.visitEnd()

        cw.visitEnd()

        return cw.toByteArray()
    }

    static byte[] generateResourceTypeClass(SymbolTable table, String packageName, String targetPackageName, ResourceType resType) {
        List<Symbol> symbols = table.getSymbolByResourceType(resType)
        if (symbols.isEmpty()) {
            return null
        }

        ClassWriter cw = new ClassWriter(COMPUTE_MAXS)
        String internalName = generateInternalName(packageName, resType)
        cw.visit(
                V1_8,
                ACC_PUBLIC + ACC_FINAL + ACC_SUPER,
                internalName, null,
                "java/lang/Object", null)

        cw.visitInnerClass(
                internalName,
                generateInternalName(packageName, null),
                resType.getName(),
                ACC_PUBLIC + ACC_FINAL + ACC_STATIC)

        for (s in symbols) {
            cw.visitField(
                    ACC_PUBLIC + ACC_STATIC,
                    s.name,
                    s.javaType.desc,
                    null,
                    null
            )

            if (s instanceof Symbol.StyleableSymbol) {
                List<String> children = s.children
                for (int i = 0; i < children.size(); i++) {
                    String child = children.get(i)

                    cw.visitField(
                            ACC_PUBLIC + ACC_STATIC,
                            "${s.name}_${SymbolUtils.canonicalizeValueResourceName(child)}",
                            "I",
                            null,
                            null)
                }
            }
        }

        // Constructor
        MethodVisitor init = cw.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null)
        init.visitCode()
        init.visitVarInsn(ALOAD, 0)
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        init.visitInsn(RETURN)
        init.visitMaxs(0, 0)
        init.visitEnd()

        // init method
        MethodVisitor clinit = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null)
        clinit.visitCode()
        for (s in symbols) {

            String targetInternalName = generateInternalName(targetPackageName, resType)
            clinit.visitFieldInsn(GETSTATIC, targetInternalName, s.name, s.javaType.desc)
            clinit.visitFieldInsn(PUTSTATIC, internalName, s.name, s.javaType.desc)

            if (s instanceof Symbol.StyleableSymbol) {
                s.children.each {
                    String name = "${s.name}_${SymbolUtils.canonicalizeValueResourceName(it)}"
                    clinit.visitFieldInsn(GETSTATIC, targetInternalName, name, "I")
                    clinit.visitFieldInsn(PUTSTATIC, internalName, name, "I")
                }
            }
        }
        clinit.visitInsn(RETURN)
        clinit.visitMaxs(0, 0)
        clinit.visitEnd()

        cw.visitEnd()

        return cw.toByteArray()
    }

    static String generateInternalName(String packageName, ResourceType type) {
        String className
        if (type == null) {
            className = "R"
        } else {
            className = "R\$" + type.getName()
        }

        if (packageName.isEmpty()) {
            return className
        } else {
            return packageName.replace(".", "/") + "/" + className
        }
    }
}
