package com.cloudbees.cd.plugins.build.allure.client

import groovy.json.JsonOutput
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import java.util.Base64.Encoder

class AllureServiceClient {

    private final HTTPBuilder client

    AllureServiceClient(String serverUrl) throws URISyntaxException {
        this.client = new HTTPBuilder(serverUrl)
    }

    void sendResults(String projectName, ArrayList<File> reportFiles) throws IOException {
        String path = "/send-results"
        ArrayList<Map<String, String>> results = buildPayload(reportFiles)

        if (results.size() == 0) {
            throw new RuntimeException("No test results were generated. Nothing to send")
        }

        HashMap<String, Object> payload = new HashMap<>()

        payload.put("results", results)
        String json = JsonOutput.toJson(payload)

        Map<String, Object> res = post(path, json, [projectName: projectName])
        String message = extractResultMessage(res)
        assert message != null
        assert message.startsWith("Results successfully sent for project_id ")
    }

    void createProject(String projectName) throws IOException {
        Map<String, Object> response = post("/projects", "{\"id\":\"" + projectName + "\"}")
        debugPrintResponseMessage(response)
    }

    boolean isProjectExists(String projectName) throws IOException {
        try {
            Map<String, Object> response = get("/projects/" + projectName)
            debugPrintResponseMessage(response)
            String resultMessage = extractResultMessage(response)
            assert resultMessage != null
            assert resultMessage.contains("Project successfully obtained")
        } catch (RuntimeException ex) {
            if (ex.getMessage().contains("Code is 404")) {
                return false
            } else {
                throw ex
            }
        }

        return true
    }

    Map<String, Object> get(String path, Map<String, String> queryParameters = [:]) throws IOException {
        def result = null
        this.client.request(Method.GET) { request ->
            uri.path = path
            uri.query = queryParameters

            requestContentType = ContentType.JSON

            response.success = { resp, decoded ->
                result = decoded
            }

            response.failure = { resp ->
                System.out.println("!!! REQUEST: " + request.toString())
                System.err.println("!!! RESPONSE: " + resp.toString())
                throw new RuntimeException(
                        "Failed to execute request. Code is "
                                + resp.status
                                + ". See the response above for details")
                println "Request failed with status ${resp.status}"
            }
        }

        return result
    }

    Map<String, Object> post(String path, String content, Map<String, String> queryParameters = [:]) throws IOException {
        def result = null
        client.request(Method.POST) { request ->
            uri.path = path
            uri.query = queryParameters
            
            body = content
            requestContentType = ContentType.JSON

            response.success = { resp, decoded ->
                result = decoded
            }

            response.failure = { resp ->
                System.out.println("!!! REQUEST: " + request.toString())
                System.err.println("!!! RESPONSE: " + resp.toString())
                throw new RuntimeException("Failed to execute request. See the response below")
            }
        }

        return result
    }

    private static String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file))
        StringBuilder stringBuilder = new StringBuilder()
        String ls = System.getProperty("line.separator")

        String line
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line)
            stringBuilder.append(ls)
        }

        return stringBuilder.toString()
    }

    private static ArrayList<Map<String, String>> buildPayload(ArrayList<File> files) {
        ArrayList<Map<String, String>> results = new ArrayList<>()
        Encoder encoder = Base64.getEncoder()

        for (File f : files) {
            String fileName = f.getName()

            String content
            try {
                content = readFile(f)
            } catch (IOException ioException) {
                System.err.println(
                        "Failed to read report file" + fileName + " : " + ioException.getMessage())
                ioException.printStackTrace()
                continue
            }

            byte[] base64 = encoder.encode(content.getBytes())

            Map<String, String> result = new LinkedHashMap<>()
            result.put("file_name", fileName)
            result.put("content_base64", new String(base64))

            results.add(result)
        }

        return results
    }

    private static void debugPrintResponseMessage(Map<String, Object> response) {
        String message = extractResultMessage(response)
        if (null == message) {
            System.out.println("Response does not contain message")
        }
        System.out.println("!!! Allure result: " + message)
    }

    private static String extractResultMessage(Map<String, Object> response) {
        if (response.containsKey("meta_data")) {
            Map<String, Object> data = (Map<String, Object>) response.get("meta_data")
            if (data.containsKey("message")) {
                return (String) data.get("message")
            }
        }
        return null
    }

    String generateReport(String projectName) throws IOException {
        Map<String, Object> res = get("/generate-report?project_id=" + projectName)
        Map<String, Object> data = (Map<String, Object>) res.get("data")
        return (String) data.get("report_url")
    }
}
