package com.cloudbees.cd.plugins.build

import com.cloudbees.cd.plugins.build.allure.SendTestReportsTask
import com.cloudbees.cd.plugins.build.specs.ConfigureTestTask
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.testing.Test

import static com.cloudbees.cd.plugins.build.allure.AllureConfiguration.injectAllureConfig

@CompileStatic
public class CDPluginBuild implements Plugin<Project> {

    public static final String NAME = "cd-plugin-build"
    public static final String allureTaskName = "sendAllureReports"
    public static final String testConfigurationTaskName = "configureTests"

    public void apply(Project project) {

        Task testTask = project.getTasksByName('test', true).first()

        // Configuring tests before running
        project.task(testConfigurationTaskName, type: ConfigureTestTask)
        testTask.dependsOn(testConfigurationTaskName)

        // Define new task for Allure
        injectAllureConfig(project)
        project.task(allureTaskName, type: SendTestReportsTask)
        testTask.finalizedBy(allureTaskName)
    }


}