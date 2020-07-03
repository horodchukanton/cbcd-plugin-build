package com.cloudbees.cd.plugins.build

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
public class CDPluginBuild implements Plugin<Project> {

    public static final String NAME = "cd-plugin-build"

    public void apply(Project project) {
//        project.extensions.create(NAME, BuildExtension)
        project.task('sendAllureReports', type: SendTestReportsTask)
    }
}