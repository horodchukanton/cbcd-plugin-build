package com.cloudbees.cd.plugins.build

import com.cloudbees.cd.plugins.build.http.AllureServiceClient
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

@CompileStatic
class SendTestReportsTask extends DefaultTask {

    private String serverUrl
    private String resultsDir
    private String projectName

    @TaskAction
    void sendReports() {
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

}
