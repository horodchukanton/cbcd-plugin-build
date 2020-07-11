package com.cloudbees.cd.plugins.build.allure

import com.cloudbees.cd.plugins.build.allure.client.AllureServiceClient
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test

@CompileStatic
class SendTestReportsTask extends DefaultTask {

    private String serverUrl = 'http://10.201.2.37:5050/allure-docker-service'
    private String resultsDir = 'build/allure-results'
    private String projectName = ''

    @TaskAction
    void sendReports() {
        if (projectName.toLowerCase() != projectName) {
            throw new RuntimeException(
                    "projectName should contains alphanumeric lowercase characters or hyphens." +
                            " For example: 'my-project-id'"
            )
        }

        if (!serverUrl || !projectName) {
            println "Skipping sendAllureResults, as options 'serverUrl' and 'projectName' are not defined"
            return
        }

        println "sendReports. URL: \'" + serverUrl + "\' project: \'" + projectName + '\''

        try {
            AllureServiceClient client = new AllureServiceClient(serverUrl)

            if (!client.isProjectExists(projectName)) {
                println "Creating project " + projectName
                client.createProject(projectName)
            }

            saveEnvironment()
            ArrayList<File> files = collectReportFiles()
            client.sendResults(projectName, files)

            for (File f : files) {
                f.delete()
            }

            String reportUrl = client.generateReport(projectName)
            println "Generated new Allure report: " + reportUrl

        } catch (IOException | RuntimeException ioe) {
            System.err.println("Failed to send results:" + ioe.getMessage())
            ioe.printStackTrace()
            System.err.println("Report files will not be deleted." +
                    " You can try to fix issue (e.g. fix the URL) and send report with './gradlew sendAllureReports'")
        }
    }

    private ArrayList<File> collectReportFiles() {
        ArrayList<File> files = new ArrayList<>()
        File dir = new File(resultsDir)
        for (File f : dir.listFiles()) {
            if (f.getName() =~ /^\.+$/) {
                continue
            }
            files.push(f)
        }
        return files
    }

    @Input
    String getServerUrl() {
        return serverUrl
    }

    void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl
    }

    @InputDirectory
    String getResultsDir() {
        return resultsDir
    }

    void setResultsDir(String resultsDir) {
        this.resultsDir = resultsDir
    }

    @Input
    String getProjectName() {
        return projectName
    }

    void setProjectName(String projectName) {
        this.projectName = projectName
    }

    File saveEnvironment() {
        Test test = project.getTasksByName("test", false).first() as Test
        Map<String, Object> env = test.getEnvironment()

        String fileContent = ""
        env.each { String k, Object v ->
            if (!v instanceof String) return

            String value = v as String
            if (k.toLowerCase() =~ /(?:password|secret)$/) value = ('*' * value.size())

            fileContent += "${k}=${value}\n"
        }

        File envFile = new File(project.getBuildDir(), "allure-results/environment.properties")
        envFile.write(fileContent)

        return envFile
    }
}
