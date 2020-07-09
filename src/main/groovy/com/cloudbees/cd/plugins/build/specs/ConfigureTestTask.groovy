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
    String environmentName = ''

    @Input
    boolean readSecrets = false

    @Input
    boolean readEnvironmentVariables = false

    @TaskAction
    void configureProject() {
        Project project = this.getProject()

        // Environment variables from file
        Test task = (Test) project.getTasksByName("test", true).first()

        if (environmentName) {
            if (readEnvironmentVariables) {
                applyEnvironmentVariables(task, 'systemtest.env', false)
            }
            if (readSecrets) {
                applyEnvironmentVariables(task, 'remote-secrets.env', true)
            }
        }

        task.testLogging.showStandardStreams = true
        task.testLogging.events("passed", "skipped", "failed")

        task.outputs.upToDateWhen { false }
        task.systemProperty("EC_SPECS_CLI", true)
    }

    private void applyEnvironmentVariables(Test task, String filename, boolean mask) {
        File envFile = resolveEnvFilepath(environmentName, filename)
        Map<String, String> env = readEnvironmentFrom(envFile)
        showEnvironmentVariables(env, mask)
        applyEnvironmentTo(task, env)
    }

    private static Map<String, String> readEnvironmentFrom(File file) {
        assert file.exists(): "File ${file.getName()} exists"

        LinkedHashMap<String, String> env = new LinkedHashMap<>()

        // Read all lines except comments or empty lines
        def lines = file.readLines().findAll { !(it.startsWith('#') || it.isEmpty()) }

        lines.each() {
            def strings = it.tokenize("=")
            env.put(strings[0], strings[1])
        }

        return env
    }

    private static void showEnvironmentVariables(Map<String, String> env, boolean mask) {
        env.each { key, value ->
            println "Environment $key=" + (mask ? ('*' * value.size()) : value)
        }
    }

    private static void applyEnvironmentTo(Test task, Map<String, String> env) {
        env.each { key, value ->
            if (System.getenv(key) != null && System.getenv(key) != '') {
                println("Environment variable $key is already defined and will not be overwritten.")
                return
            }
            task.environment(key, value)
        }
    }

    private File resolveEnvFilepath(String environmentName, String filename) {
        Project project = this.project

        File envDir = new File(project.getProjectDir(), "environments/${environmentName}")
        assert envDir.exists() && envDir.isDirectory(): "Environment ${environmentName} exists and is a directory"

        File envFile = new File(envDir, filename)
        assert envFile.exists() && envFile.isFile(): "File ${envFile.getName()} exists and is an file"

        return envFile
    }

}
