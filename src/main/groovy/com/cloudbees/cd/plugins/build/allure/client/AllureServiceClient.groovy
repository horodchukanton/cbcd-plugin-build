package com.cloudbees.cd.plugins.build.allure.client

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient

import java.nio.charset.StandardCharsets
import java.util.Base64.Encoder

@CompileStatic
class AllureServiceClient {

    private final RESTClient client

    AllureServiceClient(String serverUrl) throws URISyntaxException {
        this.client = new RESTClient(serverUrl, ContentType.JSON)
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

        Map<String, Object> res = post(path, json, [project_id: projectName])
        String message = extractResultMessage(res)
        assert message != null
        assert message.startsWith("Results successfully sent for project_id ")
    }

    void createProject(String projectName) throws IOException {
        Map<String, Object> response = post("/projects", '{"id":"' + projectName + '"}')
        debugPrintResponseMessage(response)
    }

    boolean isProjectExists(String projectName) throws IOException {
        try {
            Map<String, Object> response = get("/projects/" + projectName)
            assert response['data']['project']['id'] == projectName
        } catch (RuntimeException ex) {
            if (ex.getMessage().contains("Request failed with status 404")) {
                return false
            } else {
                throw ex
            }
        }

        return true
    }

    Map<String, Object> get(String path, Map<String, String> queryParameters = [:]) throws IOException {
        try {
            HttpResponseDecorator resp = client.get(
                    path: path,
                    query: queryParameters,
                    contentType: ContentType.JSON) as HttpResponseDecorator

            return resp.getData() as Map<String, Object>
        }
        catch (HttpResponseException ex) {
            handleRestException(ex, path, queryParameters)
        }
    }

    Map<String, Object> post(String path, String content, Map<String, String> queryParameters = [:]) throws IOException {
        try {
            HttpResponseDecorator resp = client.post(
                    body: content,
                    path: path,
                    query: queryParameters,
                    contentType: ContentType.JSON) as HttpResponseDecorator
            return resp.getData() as Map<String, Object>
        }
        catch (HttpResponseException ex) {
            handleRestException(ex, path, queryParameters)
        }
    }

    private static handleRestException(
            HttpResponseException ex, String path, Map<String, String> queryParameters
    ) throws RuntimeException {
        def resp = ex.getResponse()

        System.err.println("!!! REQUEST: " + path + " " + queryParameters.toString())
        System.err.println("!!! RESPONSE: " + resp.getData().toString())
        println "Request failed with status ${resp.status}"

        throw new RuntimeException(
                'Failed to execute request. Code is '
                        + resp.getStatusLine().toString()
                        + '. See the response above for details'
        )
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
            System.out.println("!!! Response does not contain message")
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
        Map<String, Object> res = get("/generate-report", [project_id: projectName])
        Map<String, Object> data = (Map<String, Object>) res.get("data")
        return (String) data.get("report_url")
    }

    private static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }
}
