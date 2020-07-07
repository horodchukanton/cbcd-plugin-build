package com.cloudbees.cd.plugins.build.allure

import com.cloudbees.cd.plugins.build.allure.client.AllureServiceClient
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

@CompileStatic
class SendTestReportsTask extends DefaultTask {

    private Log

    private String serverUrl = null
    private String resultsDir = 'build/allure-results'
    private String projectName = null

    @TaskAction
    void sendReports() {
        if (projectName.toLowerCase() != projectName) {
            throw new RuntimeException(
                    "projectName should contains alphanumeric lowercase characters or hyphens." +
                            " For example: 'my-project-id'"
            )
        }

        if (!serverUrl || !projectName) {
            println "Skipping sendAllureResults, as options are not defined"
            return
        }

        println "sendReports. URL: \'" + serverUrl + "\' project: \'" + projectName + '\''

        AllureServiceClient client = new AllureServiceClient(serverUrl);
        if (!client.isProjectExists(projectName)) {
            println "Creating project " + projectName
            client.createProject(projectName)
        }

        client.sendResults(projectName, collectReportFiles())
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
}
