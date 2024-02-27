import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class SetupAppsmith {
    public static void main(String[] args) {
        HttpTemplate httpTemplate = new HttpTemplate();
        // new Gson().toJson(requestBody) converts a Map to a JSON string
        AtomicReference<String> session = new AtomicReference<>("");

        // create a user
        httpTemplate.postForm("https://apps.haroun.dev/api/v1/login", "username=jerome%40haroun.dev&password=123123")
            .thenAccept(response -> {
                System.out.println("response code: " + response.statusCode());
                Optional<String> sessionCookie = response.headers().firstValue("Set-Cookie");
                if(sessionCookie.isPresent()) {
                    System.out.println("User created successfully");
                    session.set(sessionCookie.get());
                    System.out.println(session.get());
                } else {
                    System.out.println("Failed to create user");
                }
            })
            .join(); // Join to wait for the asynchronous operation to complete
    }
}

class HttpTemplate {

    private final HttpClient httpClient;

    public HttpTemplate() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public CompletableFuture<HttpResponse<String>> get(String url) {
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public CompletableFuture<HttpResponse<String>> post(String url, String body) {
        HttpRequest request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .uri(URI.create(url))
            .header("Content-Type", "application/json") // Adjust content type as needed
            .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public CompletableFuture<HttpResponse<String>> postForm(String url, String body) {
        HttpRequest request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .uri(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded") // Adjust content type as needed
            .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    // You can add more methods for different HTTP methods and customizations
}
