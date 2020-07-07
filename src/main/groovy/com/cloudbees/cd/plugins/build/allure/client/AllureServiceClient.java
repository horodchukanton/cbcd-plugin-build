package com.cloudbees.cd.plugins.build.allure.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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

  public static final MediaType JSON = MediaType.parse("application/json");
  private final URI serverUrl;
  OkHttpClient client = new OkHttpClient();

  public AllureServiceClient(String serverUrl) throws URISyntaxException {
    this.serverUrl = new URI(serverUrl);
  }

  public void sendResults(String projectName, ArrayList<File> reportFiles) throws IOException {
    String path = "/send-results?project_id=" + projectName;
    ArrayList<Map<String, String>> results = buildPayload(reportFiles);

    JSONObject payload = new JSONObject();
    payload.put("results", results);

    JSONObject res = post(path, payload.toString());
  }

  public void createProject(String projectName) throws IOException {
    JSONObject response = post("/projects", "{\"id\":\"" + projectName + "\"}");
    debugPrintResponseMessage(response);
  }

  public boolean isProjectExists(String projectName) throws IOException {
    JSONObject response = get("/projects/" + projectName);
    debugPrintResponseMessage(response);

    return response
        .getJSONObject("meta_data")
        .getString("message")
        .contains("Project successfully obtained");
  }

  private JSONObject get(String path) throws IOException {
    Request request = new Request.Builder().url(serverUrl.toString() + path).build();
    try (Response response = client.newCall(request).execute()) {
      assert response.body() != null;
      return JSONObject.fromObject(response.body().string());
    }
  }

  private JSONObject post(String path, String content) throws IOException {

    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

    Request request =
        new Request.Builder()
            .url(serverUrl.toString() + path)
            .post(RequestBody.create(bytes, JSON))
            //            .post(RequestBody.create(content, JSON))
            .addHeader("Content-Type", "application/json")
            .build();

    System.out.println("!!! REQUEST: " + request.toString());

    Response response = client.newCall(request).execute();

    System.out.println("!!! RESPONSE: " + response.toString());

    assert response.isSuccessful();
    assert response.body() != null;

    return JSONObject.fromObject(response.body().string());
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

  private void debugPrintResponseMessage(JSONObject response) {
    String message = extractResultMessage(response);
    if (null == message) {
      System.out.println("Response does not contain message");
    }
    System.out.println("!!! Allure result: " + message);
  }

  private String extractResultMessage(JSONObject response) {
    if (response.containsKey("meta_data")) {
      JSONObject data = response.getJSONObject("meta_data");
      if (data.containsKey("message")) {
        return data.getString("message");
      }
    }
    return null;
  }
}
