package com.cloudbees.cd.plugins.build.specs

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test


@CompileStatic
class ConfigureTestTask extends DefaultTask {

    @Input
    String environmentName = 'default'

    @Input
    boolean readSecrets = false

    @Input
    boolean readEnvironmentVariables = false

    // Libraries
    @Input
    String pluginsSpecCoreVersion = '1.9.2'

    @Input
    String spockLibraryVersion = '1.1-groovy-2.4'

    @Input
    String groovyVersion = '2.4.5:indy'

    @TaskAction
    public void execute() {
        Project project = this.getProject()

        ArrayList<String> dependencies = new ArrayList<>()
        dependencies.add("org.codehaus.groovy:groovy-all:" + groovyVersion)
        dependencies.add("org.spockframework:spock-core:" + spockLibraryVersion)
        dependencies.add("com.electriccloud:ec-specs-plugins-core:" + pluginsSpecCoreVersion)
        dependencies.add("org.slf4j:slf4j-api:1.7.25")
        injectSpecsDependencies(project, dependencies)

        // Environment variables from file
        Test task = (Test) project.getTasksByName("test", true).first()
        if (environmentName && readEnvironmentVariables) {
            File envFile = resolveEnvFilepath(project, environmentName, 'systemtest.env')
            addEnvironmentVariablesFromFile(task, envFile)
        }
        if (environmentName && readSecrets) {
            File envFile = resolveEnvFilepath(project, environmentName, 'remote-secrets.env')
            addEnvironmentVariablesFromFile(task, envFile)
        }

        task.testLogging.properties.replace("showStandardStreams", true)
        task.outputs.upToDateWhen { false }
        task.systemProperty("EC_SPECS_CLI", true)
    }

    public static void injectSpecsDependencies(Project project, ArrayList<String> libs) {
        final Configuration config = project.getConfigurations().create("specDependencies")
                .setVisible(false)
                .setDescription("Adding plugin specs");

        config.defaultDependencies(new Action<DependencySet>() {
            public void execute(DependencySet dependencies) {
                for (String dep : libs) {
                    dependencies.add(project.getDependencies().create(dep))
                }
            }
        });
    }

    public static void addEnvironmentVariablesFromFile(Test testTask, File file) {
        // Read all lines except comments or empty lines
        def lines = file.readLines().findAll { !(it.startsWith('#') || it.isEmpty()) }
        lines.each() {
            def strings = it.tokenize("=")
            def (key, value) = [strings[0], strings[1]]
            println "Environment $key=${'*' * value.size()}"
            testTask.environment(key, value)
        }
    }

    public static File resolveEnvFilepath(Project project, String environment, String filename) {
        return new File(project.getProject().projectDir, "environments/${environment}")
    }
}
