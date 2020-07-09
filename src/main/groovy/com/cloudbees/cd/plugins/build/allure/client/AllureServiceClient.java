package com.cloudbees.cd.plugins.build.allure.client;

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AllureServiceClient {

  public static final MediaType JSON = MediaType.parse("application/json");
  private static final OkHttpClient client = new OkHttpClient();

  private final URI serverUrl;

  public AllureServiceClient(String serverUrl) throws URISyntaxException {
    this.serverUrl = new URI(serverUrl);
  }

  public void sendResults(String projectName, ArrayList<File> reportFiles) throws IOException {
    String path = "/send-results?project_id=" + projectName;
    ArrayList<Map<String, String>> results = buildPayload(reportFiles);

    HashMap<String, Object> payload = new HashMap<>();
    payload.put("results", results);
    String json = JsonOutput.toJson(payload);

    Map<String, Object> res = post(path, json);
    String message = extractResultMessage(res);
    assert message != null;
    assert message.startsWith("Results successfully sent for project_id ");
  }

  public void createProject(String projectName) throws IOException {
    Map<String, Object> response = post("/projects", "{\"id\":\"" + projectName + "\"}");
    debugPrintResponseMessage(response);
  }

  public boolean isProjectExists(String projectName) throws IOException {
    try {
      Map<String, Object> response = get("/projects/" + projectName);
      debugPrintResponseMessage(response);
      String resultMessage = extractResultMessage(response);
      assert resultMessage != null;
      assert resultMessage.contains("Project successfully obtained");
    } catch (RuntimeException ex) {
      if (ex.getMessage().contains("Code is 404")) {
        return false;
      } else {
        throw ex;
      }
    }

    return true;
  }

  private Map<String, Object> get(String path) throws IOException {
    Request request = new Request.Builder().url(serverUrl.toString() + path).build();

    try (Response response = client.newCall(request).execute()) {

      if (response.code() != 200) {
        System.out.println("!!! REQUEST: " + request.toString());
        System.err.println("!!! RESPONSE: " + response.toString());
        throw new RuntimeException(
            "Failed to execute request. Code is "
                + response.code()
                + " "
                + response.message()
                + ". See the response above for details");
      }

      assert response.body() != null;
      return parseResponseObject(response);
    }
  }

  private Map<String, Object> post(String path, String content) throws IOException {
    Request request =
        new Request.Builder()
            .url(serverUrl.toString() + path)
            .post(RequestBody.create(content, JSON))
            .build();

    Response response = client.newCall(request).execute();

    if (response.code() >= 400) {
      System.out.println("!!! REQUEST: " + request.toString());
      System.err.println("!!! RESPONSE: " + response.toString());
      throw new RuntimeException("Failed to execute request. See the response below");
    }

    assert response.isSuccessful();
    return parseResponseObject(response);
  }

  private Map<String, Object> parseResponseObject(Response response) throws IOException {
    ResponseBody body = response.body();
    Object responseObject = new JsonSlurper().parse(body.byteStream());

    Map<String, Object> result = null;
    try {
      result = (Map<String, Object>) responseObject;
    } catch (ClassCastException cce) {
      System.err.println("Allure server returned non-JSON object string");
      System.out.println("Response was: " + body.string());
    }

    return result;
  }

  private String readFile(File file) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line = null;
      StringBuilder stringBuilder = new StringBuilder();
      String ls = System.getProperty("line.separator");
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line);
        stringBuilder.append(ls);
      }

      return stringBuilder.toString();
    }
  }

  private ArrayList<Map<String, String>> buildPayload(ArrayList<File> files) {

    ArrayList<Map<String, String>> results = new ArrayList<>();
    Encoder encoder = Base64.getEncoder();

    for (File f : files) {
      String fileName = f.getName();

      String content;
      try {
        content = readFile(f);
      } catch (IOException ioException) {
        System.err.println(
            "Failed to read report file" + fileName + " : " + ioException.getMessage());
        ioException.printStackTrace();
        continue;
      }

      byte[] base64 = encoder.encode(content.getBytes());

      Map<String, String> result = new LinkedHashMap<>();
      result.put("file_name", fileName);
      result.put("content_base64", new String(base64));

      results.add(result);
    }

    return results;
  }

  private void debugPrintResponseMessage(Map<String, Object> response) {
    String message = extractResultMessage(response);
    if (null == message) {
      System.out.println("Response does not contain message");
    }
    System.out.println("!!! Allure result: " + message);
  }

  private String extractResultMessage(Map<String, Object> response) {
    if (response.containsKey("meta_data")) {
      Map<String, Object> data = (Map<String, Object>) response.get("meta_data");
      if (data.containsKey("message")) {
        return (String) data.get("message");
      }
    }
    return null;
  }

  public String generateReport(String projectName) throws IOException {
    Map<String, Object> res = get("/generate-report?project_id=" + projectName);
    Map<String, Object> data = (Map<String, Object>) res.get("data");
    return (String) data.get("report_url");
  }
}
