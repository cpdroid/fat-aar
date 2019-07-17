package com.cpdroid.fat_aar

import com.google.common.io.Files
import groovy.transform.CompileStatic
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedModuleVersion
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier

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
        return null;
    }

    @Override
    String getName() {
        return mFile.getName()
    }

    @Override
    String getType() {
        return Files.getFileExtension(getName())
    }

    @Override
    String getExtension() {
        return Files.getFileExtension(getName())
    }

    @Nullable
    @Override
    String getClassifier() {
        return null
    }

    @Override
    ComponentArtifactIdentifier getId() {
        return null
    }
}
