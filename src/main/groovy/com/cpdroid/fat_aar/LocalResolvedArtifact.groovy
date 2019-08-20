package com.cpdroid.fat_aar

import com.google.common.io.Files
import groovy.transform.CompileStatic
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedModuleVersion
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.internal.artifacts.ivyservice.modulecache.dynamicversions.DefaultResolvedModuleVersion
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier

import javax.annotation.Nullable

@CompileStatic
class LocalResolvedArtifact implements ResolvedArtifact {

    private File mFile

    LocalResolvedArtifact(File file) {
        mFile = file
    }

    @Override
    String toString() {
        return getName() + " (" + mFile.getAbsolutePath() + ")"
    }

    @Override
    File getFile() {
        return mFile
    }

    @Override
    ResolvedModuleVersion getModuleVersion() {
        return new DefaultResolvedModuleVersion(
                DefaultModuleVersionIdentifier.newId("unspecified", getName(), "unspecified"))
    }

    @Override
    String getName() {
        return Files.getNameWithoutExtension(mFile.getName())
    }

    @Override
    String getType() {
        return Files.getFileExtension(mFile.getName())
    }

    @Override
    String getExtension() {
        return Files.getFileExtension(mFile.getName())
    }

    @Nullable
    @Override
    String getClassifier() {
        return null
    }

    @Override
    ComponentArtifactIdentifier getId() {
        return new OpaqueComponentArtifactIdentifier(mFile)
    }
}
