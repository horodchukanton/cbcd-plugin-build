package com.cloudbees.cd.plugins.build

import com.cloudbees.cd.plugins.build.allure.SendTestReportsTask
import com.cloudbees.cd.plugins.build.specs.ConfigureTestTask
//import com.cloudbees.pdk.hen.plugin.GenerateClassesTask
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import static com.cloudbees.cd.plugins.build.allure.AllureConfiguration.injectAllureConfig

@CompileStatic
class CDPluginBuild implements Plugin<Project> {

    public static final String NAME = "cd-plugin-build"
    public static final String allureTaskName = "sendAllureReports"
    public static final String testConfigurationTaskName = "configureTests"
    public static final String henTaskName = "generateHenClasses"
//    public static final String injectDependenciesTaskName = "injectDependencies"

    void apply(Project project) {

        project.plugins.apply('groovy')

        Task configTask = project.task(testConfigurationTaskName, type: ConfigureTestTask)

//        Task hen = project.task(henTaskName, type: GenerateClassesTask)

        // Dependencies should run right now
        // Task depsTask = project.getTasksByName('dependencies', false).first()
        // project.task(injectDependenciesTaskName, type: InjectDependenciesTask)
        // depsTask.dependsOn(injectDependenciesTaskName)

        boolean allureDisabled = project.hasProperty('disableAllure')

        if (!allureDisabled){
            injectAllureConfig(project)
            project.task(allureTaskName, type: SendTestReportsTask)
        }

        project.afterEvaluate {
            Task testTask = project.getTasksByName('test', false).first()

            // Configuring tests before running
            testTask.dependsOn(testConfigurationTaskName)

            // Define new task for Allure
            if (!allureDisabled){
                Task allureTask = project.getTasksByName(allureTaskName, false)?.first()
                testTask.finalizedBy(allureTaskName)
                allureTask.mustRunAfter('test')
            }
        }

    }


}