package com.cloudbees.cd.plugins.build

import com.cloudbees.cd.plugins.build.allure.SendTestReportsTask
import com.cloudbees.cd.plugins.build.specs.ConfigureTestTask
import groovy.transform.CompileStatic

//import com.cloudbees.cd.plugins.build.specs.InjectDependenciesTask

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import static com.cloudbees.cd.plugins.build.allure.AllureConfiguration.injectAllureConfig

@CompileStatic
public class CDPluginBuild implements Plugin<Project> {

    public static final String NAME = "cd-plugin-build"
    public static final String allureTaskName = "sendAllureReports"
    public static final String testConfigurationTaskName = "configureTests"
//    public static final String injectDependenciesTaskName = "injectDependencies"

    public void apply(Project project) {

        project.plugins.apply('groovy')
        project.plugins.apply('idea')

        // Dependencies should run right now
        //        Task depsTask = project.getTasksByName('dependencies', false).first()
//        project.task(injectDependenciesTaskName, type: InjectDependenciesTask)
//        depsTask.dependsOn(injectDependenciesTaskName)

        injectAllureConfig(project)

        project.afterEvaluate {
            Task testTask = project.getTasksByName('test', false).first()

            // Configuring tests before running
            project.task(testConfigurationTaskName, type: ConfigureTestTask)
            testTask.dependsOn(testConfigurationTaskName)

            // Define new task for Allure
            Task allureTask = project.task(allureTaskName, type: SendTestReportsTask)
            testTask.finalizedBy(allureTaskName)
            allureTask.mustRunAfter('test')
        }

    }


}