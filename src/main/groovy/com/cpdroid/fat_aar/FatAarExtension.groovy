package com.cpdroid.fat_aar

import groovy.transform.CompileStatic

@CompileStatic
class FatAarExtension {
    boolean verboseLog = false
    boolean ignoreAndroidSupport = true
    private List<String> ignoreDependencies = new ArrayList<>()

    void ignoreDependencies(String... dependency) {
        Utils.logLevel2(dependency)
        for (String dep : dependency) {
            if (!ignoreDependencies.contains(dep))
                ignoreDependencies.add(dep)
        }
    }

    List<String> getIgnoreDependencies() {
        return ignoreDependencies
    }
}
