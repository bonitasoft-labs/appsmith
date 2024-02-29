import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SetupAppsmith {
    public static void main(String[] args) throws JSONException, IOException {

        HttpTemplate httpTemplate = new HttpTemplate();

        // login User
        HttpResponse<String> loginResponse = httpTemplate.sendRequest(
            "https://apps.haroun.dev/api/v1/login", "POST",
            "username=meee1%40haroun.dev&password=123123",
            "application/x-www-form-urlencoded");
        System.out.println("response code: " + loginResponse.statusCode());
        Optional<String> sessionCookie = loginResponse.headers().firstValue("Set-Cookie");
        if (sessionCookie.isPresent()) {
            System.out.println("User login success");
            httpTemplate.setCookie(sessionCookie.get());

        } else {
            System.out.println("User login failed");
            // create user
            System.out.println("Try Create the user first");
            HttpResponse<String> createUserResponse = httpTemplate.sendRequest(
                "https://apps.haroun.dev/api/v1/users", "POST",
                "email=meee1%40haroun.dev&password=123123",
                "application/x-www-form-urlencoded");
            System.out.println("response code: " + loginResponse.statusCode());
            sessionCookie = createUserResponse.headers().firstValue("Set-Cookie");
            if (sessionCookie.isPresent()) {
                System.out.println("User creation & login success");
                httpTemplate.setCookie(sessionCookie.get());

            } else {
                System.out.println("User creation failed");
                return;
            }
        }

        HttpResponse<String> workspacesResponse = httpTemplate.sendRequest(
            "https://apps.haroun.dev/api/v1/workspaces/home", "GET",
            null, "application/json");
        if (workspacesResponse.statusCode() == 200) {
            JSONObject responseBody = new JSONObject(workspacesResponse.body());
            // extracts the workspaces from the response
            String workspaceId = "";
            JSONArray workspaces = responseBody.getJSONArray("data");
            if (workspaces.length() == 0) {
                Map<String, String> newWorkspace = Map.of("name", "My Workspace");
                // create a workspace
                HttpResponse<String> createWorkspaceResponse = httpTemplate.sendRequest("https://apps.haroun.dev/api/v1/workspaces", "POST",
                    new JSONObject(newWorkspace).toString(), "application/json");

                if (createWorkspaceResponse.statusCode() == 201) {
                    System.out.println("Workspace created successfully");
                } else {
                    System.out.println("Failed to create workspace");
                }// Join to wait for the asynchronous operation to complete
            }else {
                //  rename the first workspace
                JSONObject workspace = workspaces.getJSONObject(0);
                workspaceId = workspace.getString("id");
                workspace.put("name", "My Workspace");
                HttpResponse<String> updateWorkspaceResponse = httpTemplate.sendRequest(
                    "https://apps.haroun.dev/api/v1/workspaces/" + workspaceId, "PUT",
                    workspace.toString(), "application/json");
                if (updateWorkspaceResponse.statusCode() == 200) {
                    System.out.println("Workspace renamed successfully");
                } else {
                    System.out.println("Failed to rename workspace");
                }
            }

            // import apps files
            File apps = new File("/Users/haroun.elalami/_bonita/_github/appsmith/automate/apps");
            for (File app : apps.listFiles()) {
                if (app.isFile()) {
                    String appName = app.getName().split("\\.")[0];
                    // required custom class to manage multipart form data
                    MultiPartBodyPublisher publisher = new MultiPartBodyPublisher()
                        .addPart("file", app.toPath());

                    HttpResponse<String> importAppResponse = httpTemplate.sendRequest(
                        "https://apps.haroun.dev/api/v1/applications/import/" + workspaceId, "POST",
                        publisher.build(), "multipart/form-data; boundary=" + publisher.getBoundary());
                    if (importAppResponse.statusCode() == 200) {
                        System.out.println("App " + appName + ":" + new JSONObject(importAppResponse.body()).getJSONObject("data").getJSONObject("application").getString("id") + " imported successfully");
                    } else {
                        System.out.println("Failed to import app " + appName);
                        System.out.println(importAppResponse.body());
                    }
                }

            }
        }

        /* OkHttp example that works
        OkHttpClient client = new OkHttpClient().newBuilder()

            .build();
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file","App 1.json",
                RequestBody.create(MediaType.parse("application/json"),
                    new File("/Users/haroun.elalami/_bonita/_github/appsmith/automate/apps/App 1.json")))
            .build();

        Request request = new Request.Builder()
            .url("https://apps.haroun.dev/api/v1/applications/import/65def189b2a273739df8fa9c")
            .method("POST", body)
            .addHeader("x-anonymous-user-id", "bc8d6ea8-9882-4c97-a78a-c1754aa6dafd")
            .addHeader("x-requested-by", "Appsmith")
            .addHeader("Cookie", "SESSION=977f276b-6d08-4e34-a67a-0d65b4b3a1dd")
            .build();
        Response response = client.newCall(request).execute();

        System.out.println(response.body().string());
        */
    }

    public static byte[] createMultipartFormDataBody(Path filePath) throws IOException {
        // Read the file content
        byte[] fileBytes = Files.readAllBytes(filePath);

        // Construct the multipart/form-data body
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(UUID.randomUUID()).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filePath.getFileName()).append("\"\r\n");
        sb.append("Content-Type: application/json\r\n");
        sb.append("\r\n");

        // Combine the parts into a single byte array
        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] endBytes = ("\r\n--" + UUID.randomUUID() + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[headerBytes.length + fileBytes.length + endBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(endBytes, 0, body, headerBytes.length + fileBytes.length, endBytes.length);

        return body;
    }
}

class HttpTemplate {
    private final HttpClient httpClient;
    private String cookie = "";

    public HttpTemplate() throws IOException {
        this.httpClient = HttpClient.newHttpClient();
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public HttpResponse<String> sendRequest(String url, String method, Object body, String contentType) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .header("Cookie", cookie)
            // can be any id
            .header("x-anonymous-user-id", "bc8d6ea8-9882-4c97-a78a-c1754aa6dafd")
            .header("x-requested-by", "Appsmith")
            .uri(URI.create(url));

        switch (method.toUpperCase()) {
            case "GET":
                requestBuilder.GET();
                break;
            case "PUT":
                requestBuilder.PUT(prepareBody(body));
                break;
            case "POST":
                requestBuilder.POST(prepareBody(body));
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        if (body != null && contentType != null) {
            requestBuilder.header("Content-Type", contentType);
        }

        HttpRequest request = requestBuilder.build();

        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error sending HTTP request: " + e.getMessage(), e);
        }
    }

    private HttpRequest.BodyPublisher prepareBody(Object body) {
        return switch (body) {
            case String s -> HttpRequest.BodyPublishers.ofString(s);
            case byte[] bytes -> HttpRequest.BodyPublishers.ofByteArray(bytes);
            case HttpRequest.BodyPublisher bodyPublisher -> bodyPublisher;
            case null, default ->
                    throw new IllegalArgumentException("Unsupported body type: " + body.getClass().getName());
        };
    }

    public CompletableFuture<HttpResponse<String>> get(String url) {
        HttpRequest request = HttpRequest.newBuilder()
            .header("Cookie", cookie)
            .GET()
            .uri(URI.create(url))
            .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public CompletableFuture<HttpResponse<String>> post(String url, String body) {
        HttpRequest request = HttpRequest.newBuilder()
            .header("Cookie", cookie)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .uri(URI.create(url))
            .header("Content-Type", "application/json") // Adjust content type as needed
            .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public CompletableFuture<HttpResponse<String>> postForm(String url, String body) {
        HttpRequest request = HttpRequest.newBuilder()
            .header("Cookie", cookie)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .uri(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded") // Adjust content type as needed
            .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    // You can add more methods for different HTTP methods and customizations
}
