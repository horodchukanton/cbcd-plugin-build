package com.cloudbees.cd.plugins.build.specs


import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test

@CompileStatic
class ConfigureTestTask extends DefaultTask {

    @Input
    String environmentName = null

    @Input
    boolean readSecrets = false

    @Input
    boolean readEnvironmentVariables = false

    @TaskAction
    public void configureProject() {
        Project project = this.getProject()

        // Environment variables from file
        Test task = (Test) project.getTasksByName("test", true).first()

        if (environmentName && readEnvironmentVariables) {
            File envFile = resolveEnvFilepath(project, environmentName, 'systemtest.env')
            addEnvironmentVariablesFromFile(task, envFile, false)
        }
        if (environmentName && readSecrets) {
            File envFile = resolveEnvFilepath(project, environmentName, 'remote-secrets.env')
            addEnvironmentVariablesFromFile(task, envFile, true)
        }

        task.testLogging.showStandardStreams = true
        task.testLogging.events("passed", "skipped", "failed")

        task.outputs.upToDateWhen { false }
        task.systemProperty("EC_SPECS_CLI", true)
    }


    public static void addEnvironmentVariablesFromFile(Test testTask, File file, boolean mask) {
        assert file.exists(): "File ${file.getName()} exists"
        // Read all lines except comments or empty lines
        def lines = file.readLines().findAll { !(it.startsWith('#') || it.isEmpty()) }
        lines.each() {
            def strings = it.tokenize("=")
            def (key, value) = [strings[0], strings[1]]
            println "Environment $key=" + (mask ? ('*' * value.size()) : value)

            if (System.getenv(key) != null && System.getenv(key) != ''){
                println("Environment variable $key is already defined and will not be overwritten.")
            }

            testTask.environment(key, value)
        }
    }

    public static File resolveEnvFilepath(Project project, String environment, String filename) {
        File envDir = new File(project.getProject().projectDir, "environments/${environment}")
        assert envDir.exists() && envDir.isDirectory(): "Environment ${environment} exists and is a directory"

        File envFile = new File(envDir, filename)
        assert envFile.exists() && envFile.isFile(): "File ${envFile.getName()} exists and is an file"

        return envFile
    }
}
