package com.cloudbees.cd.plugins.build.specs

import groovy.json.JsonSlurper
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
    String secretsProject = 'flow-plugin-team-test-harness'

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

        env.each { String k, String v ->
            if (System.getenv(k) != null && System.getenv(k) != '') {
                println("Environment variable $k is already defined and will not be overwritten.")
                env.remove(k)
                v = System.getenv(k)
            }

            EnvironmentContainer.addVar(k, (mask ? ('*' * v.size()) : v))
        }

        showEnvironmentVariables(env, mask)
        applyEnvironmentTo(task, env)
    }

    private Map<String, String> readEnvironmentFrom(File file) {
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

    private void showEnvironmentVariables(Map<String, String> env, boolean mask) {
        env.each { key, value ->
            println "Environment $key=" + (mask ? ('*' * value.size()) : value)
        }
    }

    private void applyEnvironmentTo(Test task, Map<String, String> env) {
        env.each { key, value ->
            if (value =~ /GCP-SECRET/) {
                try {
                    value = resolveSecret(value)
                    println "Resolved secret $key to ${'*' * value.size()}"
                } catch (Throwable e) {
                    println("Failed to resolve secret: $e.message")
                }
            }
            task.environment(key, value)
        }
    }

    private String resolveSecret(String value) {
        println "Using project $secretsProject"
        String secretName = value.replaceAll(/\(\(GCP-SECRET:\s+/, '').replaceAll(/\)\)/, '')
        println "Trying to resolve secret $secretName"
        String versionsRaw = executeCommand("gcloud", "--project", secretsProject,
                "beta", "secrets", "versions", "list", secretName, "--format", "json")
        List<Map> versions = new JsonSlurper().parseText(versionsRaw) as List<Map>
        int latestVersion = 0
        for (Map version in versions) {
            if (version.get('state') == 'ENABLED') {
                String name = version.name
                int v = name.split(/\//).last() as int
                if (v > latestVersion) {
                    latestVersion = v
                }
            }
        }

        if (latestVersion) {
            println "Found secret version $latestVersion"
            String secret = executeCommand("gcloud",
                    "--project",
                    secretsProject,
                    "beta",
                    "secrets",
                    "versions",
                    "access",
                    "--secret",
                    secretName,
                    latestVersion as String)
            return secret.trim()
        }
        return value
    }

    private static String executeCommand(String... cmd) {
        ProcessBuilder pb = new ProcessBuilder(cmd)
        Process process = pb.start()

        BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()))
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()))
        int code = process.waitFor()

        StringBuilder out = new StringBuilder()
        String line
        while (line = stdOut.readLine()) {
            out.append(line)
            out.append(System.lineSeparator())
        }
        StringBuilder err = new StringBuilder()
        while (line = stdError.readLines()) {
            err.append(line)
            err.append(System.lineSeparator())
        }
        if (code != 0) {
            throw new RuntimeException("Failed to execute command: exit code $code, stderr: $err")
        }
        return out.toString()
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
