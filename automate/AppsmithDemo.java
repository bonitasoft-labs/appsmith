import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class AppsmithDemo {
    public static void main(String[] args) throws JSONException, IOException {
        final String BASE_URL;
        final String DEFAULT_USERNAME = "dev@dev.com";
        final String DEFAULT_PASSWORD = "123123";

        final String DEFAULT_APPSMITH_URL = "http://app-smith-dev.localhost:8070";
        final String DEFAULT_APP_NAME = "My App";
        final String DEFAULT_APPSMITH_WORKSPACE_NAME = "My Workspace";

        String appsmithUrl = System.getenv("APPSMITH_URL");
        if (appsmithUrl == null) {
            appsmithUrl = DEFAULT_APPSMITH_URL;
        }
        BASE_URL = appsmithUrl + "/api/v1/";

        String username = DEFAULT_USERNAME;
        String password = DEFAULT_PASSWORD;

        String workspaceName = System.getenv("APPSMITH_WORKSPACE_NAME");
        if (workspaceName == null) {
            workspaceName = DEFAULT_APPSMITH_WORKSPACE_NAME;
        }
        String appName = System.getenv("APP_NAME");
        if (appName == null) {
            appName = DEFAULT_APP_NAME;
        }


        HttpUtils httpTemplate = new HttpUtils();

        // login User
        HttpResponse<String> loginResponse = httpTemplate.sendRequest(
            BASE_URL + "login", "POST",
            "username=" + username + "&password=" + password,
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
                "email=" + username + "&password=" + password,
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
                Map<String, String> newWorkspace = Map.of("name", workspaceName);
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
                workspace.put("name", workspaceName);
                HttpResponse<String> updateWorkspaceResponse = httpTemplate.sendRequest(
                    BASE_URL + "workspaces/" + workspaceId, "PUT",
                    workspace.toString(), "application/json");
                if (updateWorkspaceResponse.statusCode() == 200) {
                    System.out.println("Workspace renamed successfully");
                } else {
                    System.out.println("Failed to rename workspace");
                }
             }

            // Get the applications of the workspace
            HttpResponse<String> appsResponse = httpTemplate.sendRequest(
                BASE_URL + "applications/home?workspaceId=" + workspaceId, "GET",
                null, "application/json");
            JSONArray applications = null;
            String newAppId = null;
            if (workspacesResponse.statusCode() == 200) {
                JSONObject responseBodyApps = new JSONObject(appsResponse.body());
                applications = responseBodyApps.getJSONArray("data");
                if (applications.isEmpty()) {
                    System.out.println("No apps found in the workspace");
                }
                // Check if the application already exists, and delete the default app if any
                boolean appFound = false;
                for (int ii = 0; ii < applications.length(); ii++) {
                    JSONObject app = applications.getJSONObject(ii);
                    String defaultInitialAppName = "My first application";
                    if (appName.equals(app.getString("name"))) {
                        System.out.println("App " + appName + " already exists");
                        appFound = true;
                    } else if (defaultInitialAppName.equals(app.getString("name"))) {
                        // Delete the default app
                        HttpResponse<String> deleteAppResponse = httpTemplate.sendRequest(
                            BASE_URL + "applications/" + app.getString("id"), "DELETE",
                            null, "application/json");
                        if (deleteAppResponse.statusCode() == 200) {
                            System.out.println("Default app deleted successfully");
                        } else {
                            System.out.println("Failed to delete default app");
                        }
                    }
                }
                if (!appFound) {
                    // Create the application
                    Map<String, String> newApplication = Map.of("name", appName);
                    HttpResponse<String> createAppResponse = httpTemplate.sendRequest(
                        BASE_URL + "applications?workspaceId=" + workspaceId, "POST",
                        new JSONObject(newApplication).toString(), "application/json");
                    if (createAppResponse.statusCode() == 201) {
                        System.out.println("Application " + appName + " created successfully");
                        newAppId = new JSONObject(createAppResponse.body()).getJSONObject("data").getString("id");
                    } else {
                        System.out.println("Failed to create application " + appName);
                    }
                }
            }

            // Publish an app
            if (newAppId != null) {
                HttpResponse<String> publishResponse = httpTemplate.sendRequest(
                    BASE_URL + "applications/publish/" + newAppId, "POST",
                    "", "application/json");
                if (publishResponse.statusCode() == 200) {
                    System.out.println("App " + appName + " published successfully");
                } else {
                    System.out.println("Failed to publish app " + appName);
                }
            }

        }
    }
}

class HttpUtils {
    private final HttpClient httpClient;
    private String cookie = "";

    public HttpUtils() throws IOException {
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
            case "DELETE":
                requestBuilder.DELETE();
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
