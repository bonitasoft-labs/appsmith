import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class SetupAppsmith {
    public static void main(String[] args) throws JSONException, IOException {
        final String BASE_URL = "http://app-smith-dev.localhost:8070/api/v1/";
        final String USERNAME = "walter.bates@bonitasoft.com";
        final String PASSWORD = "bpmbpm";

        HttpTemplate httpTemplate = new HttpTemplate();

        // login User
        HttpResponse<String> loginResponse = httpTemplate.sendRequest(
            BASE_URL + "login", "POST",
            "username=" + USERNAME + "&password=" + PASSWORD,
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
                BASE_URL + "users", "POST",
                "email=" + USERNAME + "&password=" + PASSWORD,
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
            BASE_URL + "workspaces/home", "GET",
            null, "application/json");
        if (workspacesResponse.statusCode() == 200) {
            JSONObject responseBody = new JSONObject(workspacesResponse.body());
            // extracts the workspaces from the response
            String workspaceId = "";
            JSONArray workspaces = responseBody.getJSONArray("data");
            if (workspaces.isEmpty()) {
                Map<String, String> newWorkspace = Map.of("name", "My Workspace");
                // create a workspace
                HttpResponse<String> createWorkspaceResponse = httpTemplate.sendRequest(BASE_URL + "workspaces", "POST",
                    new JSONObject(newWorkspace).toString(), "application/json");

                if (createWorkspaceResponse.statusCode() == 201) {
                    System.out.println("Workspace created successfully");
                } else {
                    System.out.println("Failed to create workspace");
                }// Join to wait for the asynchronous operation to complete
            } else {
                //  rename the first workspace
                JSONObject workspace = workspaces.getJSONObject(0);
                workspaceId = workspace.getString("id");
                workspace.put("name", "My Workspace");
                HttpResponse<String> updateWorkspaceResponse = httpTemplate.sendRequest(
                    BASE_URL + "workspaces/" + workspaceId, "PUT",
                    workspace.toString(), "application/json");
                if (updateWorkspaceResponse.statusCode() == 200) {
                    System.out.println("Workspace renamed successfully");
                } else {
                    System.out.println("Failed to rename workspace");
                }
            }

            // import apps files
            File apps = new File(SetupAppsmith.class.getResource("apps").getPath());
            for (File app : apps.listFiles()) {
                if (app.isFile()) {
                    String appName = app.getName().split("\\.")[0];
                    // required custom class to manage multipart form data
                    MultiPartBodyPublisher publisher = new MultiPartBodyPublisher()
                        .addPart("file", app.toPath());

                    HttpResponse<String> importAppResponse = httpTemplate.sendRequest(
                        BASE_URL + "applications/import/" + workspaceId, "POST",
                        publisher.build(), "multipart/form-data; boundary=" + publisher.getBoundary());
                    if (importAppResponse.statusCode() == 200) {
                        System.out.println("App " + appName + ":" +
                            new JSONObject(importAppResponse.body()).getJSONObject("data")
                                .getJSONObject("application").getString("id") + " imported successfully");
                    } else {
                        System.out.println("Failed to import app " + appName);
                        System.out.println(importAppResponse.body());
                    }
                }
            }

            // Get the applications of the workspace
            HttpResponse<String> appsResponse = httpTemplate.sendRequest(
                BASE_URL + "applications/home?workspaceId=" + workspaceId, "GET",
                null, "application/json");
            JSONArray applications = null;
            if (workspacesResponse.statusCode() == 200) {
                JSONObject responseBodyApps = new JSONObject(appsResponse.body());
                applications = responseBodyApps.getJSONArray("data");
                if (applications.isEmpty()) {
                    System.out.println("No apps found in the workspace");
                }
            }

            // Publish an app
            if (applications != null) {
                String firstAppId = applications.getJSONObject(0).getString("id");
                HttpResponse<String> publishResponse = httpTemplate.sendRequest(
                    BASE_URL + "applications/publish/" + firstAppId, "POST",
                    "", "application/json");
                if (publishResponse.statusCode() == 200) {
                    System.out.println("App " + firstAppId + " published successfully");
                } else {
                    System.out.println("Failed to publish app " + firstAppId);
                }
            }

            // Export an app
            if (applications != null) {
                String firstAppId = applications.getJSONObject(0).getString("id");
                HttpResponse<String> exportResponse = httpTemplate.sendRequest(
                    BASE_URL + "applications/export/" + firstAppId, "GET",
                    "", "application/json");
                if (exportResponse.statusCode() == 200 && exportResponse.body() != null) {
                    System.out.println("App " + firstAppId + " exported successfully");
                } else {
                    System.out.println("Failed to export app " + firstAppId);
                }
            }
        }
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
        if (body instanceof String) {
            return HttpRequest.BodyPublishers.ofString((String) body);
        } else if (body instanceof byte[]) {
            return HttpRequest.BodyPublishers.ofByteArray((byte[]) body);
        } else if (body instanceof HttpRequest.BodyPublisher) {
            return (HttpRequest.BodyPublisher) body;
        } else {
            throw new IllegalArgumentException("Unsupported body type: " + body.getClass().getName());
        }
    }

}
