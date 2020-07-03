package com.cloudbees.cd.plugins.build.http;

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
import java.util.LinkedHashMap;
import java.util.Map;
import net.sf.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AllureServiceClient {

  private final URI serverUrl;
  OkHttpClient client = new OkHttpClient();
  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  public AllureServiceClient(String serverUrl) throws URISyntaxException {
    this.serverUrl = new URI(serverUrl);
  }

  public void sendResults(String projectName, ArrayList<File> reportFiles) {
    String path = "/send-results?project_id=" + projectName;
    ArrayList<Map<String, String>> results = buildPayload(reportFiles);

    JSONObject payload = new JSONObject();
    payload.put("results", results);

    try {
      post(path, payload.toString());
    } catch (IOException ioException) {
      System.err.println("Failed to send results: " + ioException.getMessage());
    }
  }

  public void createProject(String projectName) throws IOException {
    post("/projects", "{\"id\"}:\"" + projectName + "\"");
  }

  public boolean isProjectExists(String projectName) {
    try {
      Object response = get("/projects/" + projectName);
      assert response != null;
    } catch (IOException ioException) {
      System.err.println("Failed to request if project exists: " + ioException.getMessage());
      return false;
    }
    return true;
  }

  private Object get(String path) throws IOException {
    Request request = new Request.Builder().url(serverUrl.toString() + path).build();

    try (Response response = client.newCall(request).execute()) {
      assert response.body() != null;
      return new JsonSlurper().parseText(response.body().string());
    }
  }

  private Object post(String path, String content) throws IOException {
    RequestBody body = RequestBody.create(content, JSON);
    Request request = new Request.Builder().url(serverUrl.toString() + path).post(body).build();
    Response response = client.newCall(request).execute();
    assert response.body() != null;
    return new JsonSlurper().parseText(response.body().string());
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
        ioException.printStackTrace();
        System.err.println(
            "Failed to read report file" + fileName + " : " + ioException.getMessage());
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
}
