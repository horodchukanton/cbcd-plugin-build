package com.cloudbees.cd.plugins.build.allure

import com.cloudbees.cd.plugins.build.allure.client.AllureServiceClient
import com.cloudbees.cd.plugins.build.specs.EnvironmentContainer
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class SendTestReportsTask extends DefaultTask {

    private String serverUrl = project.findProperty('allureReportsServerUrl') ?: 'http://10.201.2.37:5050/allure-docker-service'
    private String projectName = ''

    @Input
    String reportsBaseUrl = 'https://plugin-reports.nimbus.beescloud.com/allure-docker-service/'

    @TaskAction
    void sendReports() {
        if (projectName.toLowerCase() != projectName) {
            throw new RuntimeException(
                    "projectName should contains alphanumeric lowercase characters or hyphens." +
                            " For example: 'my-project-id'"
            )
        }

        if (!serverUrl || !projectName) {
            println "Will not run sendAllureReports, as options 'serverUrl' and 'projectName' are not defined"
            return
        }

        println "Reporting to URL: \'" + serverUrl + "\' with a project: \'" + projectName + '\''

        try {
            AllureServiceClient client = new AllureServiceClient(serverUrl)

            if (!client.isServerAccessible()){
                println("Failed to connect the Allure Reports Server. Skipping")
                return
            }

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

            String publicReportsUrl = transformToPublicUrl(reportUrl, reportsBaseUrl)

            println "Generated new Allure report: " + publicReportsUrl

        } catch (IOException | RuntimeException ioe) {
            System.err.println("Failed to send results:" + ioe.getMessage())
            ioe.printStackTrace()
            System.err.println("Report files will not be deleted." +
                    " You can try to fix issue (e.g. fix the URL) and send report with './gradlew sendAllureReports'")
        }
    }

    private ArrayList<File> collectReportFiles() {
        ArrayList<File> files = new ArrayList<>()
        File dir = new File(this.project.getBuildDir(), 'allure-results')
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

    @Input
    String getProjectName() {
        return projectName
    }

    void setProjectName(String projectName) {
        this.projectName = projectName
    }

    File saveEnvironment() {
        Map<String, String> env = EnvironmentContainer.getAll()

        String fileContent = ""
        env.each { String k, Object v ->
            String value = v as String
            fileContent += "${k}=${value}\n"
        }

        File envFile = new File(project.getBuildDir(), "allure-results/environment.properties")
        envFile.write(fileContent)

        return envFile
    }

    static String transformToPublicUrl(String reportUrl, String newBaseUrl) {

        // Transforming for easy access to different parts
        URL reportURI = new URL(reportUrl)
        URL newBaseUri = new URL(newBaseUrl)

        String resultFilepath = reportURI.file

        // Looking for a root file path
        String[] segments = reportURI.file.split('/')
        int index = -1
        for (int i = 0; i < segments.size(); i++){
            if (segments[i].equals("projects")) {
                index = i;
                break
            }
        }

        // Joining new URL path with all that goes after root
        if (index != -1){
            resultFilepath = newBaseUri.path + segments[index..(segments.size() -1)].join('/')
        }

        URL result = new URL(newBaseUri.protocol, newBaseUri.host, newBaseUri.port, resultFilepath)



        return result.toString()
    }
}
