import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * A lightweight Java-only Web Server that serves the basic frontend
 * files and calculates friend recommendation scores dynamically via a REST API.
 */
public class WebServer {

    // User Model
    static class User {
        String id;
        String name;
        List<String> skills;
        String city;
        List<String> friends;

        User(String id, String name, List<String> skills, String city) {
            this.id = id;
            this.name = name;
            this.skills = skills;
            this.city = city;
            this.friends = new ArrayList<>();
        }
    }

    private static final Map<String, User> database = new HashMap<>();

    public static void main(String[] args) throws IOException {
        initializeDatabase();

        // Start HTTP Server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Static file handlers
        server.createContext("/", new FileHandler("index.html", "text/html"));
        server.createContext("/styles.css", new FileHandler("styles.css", "text/css"));
        
        // API endpoint handler
        server.createContext("/api/recommend", new RecommendationHandler());

        server.setExecutor(null); // default executor
        System.out.println("Java Web Server started successfully!");
        System.out.println("Open your browser and navigate to: http://localhost:8080");
        server.start();
    }

    private static void initializeDatabase() {
        User rahul = new User("rahul", "Rahul Sharma", Arrays.asList("Java", "Python"), "Hyderabad");
        User priya = new User("priya", "Priya Reddy", Arrays.asList("Python", "AI"), "Hyderabad");
        User arjun = new User("arjun", "Arjun Kumar", Arrays.asList("JavaScript", "Web Development"), "Bangalore");
        User sneha = new User("sneha", "Sneha Patel", Arrays.asList("AI", "Machine Learning", "Python"), "Hyderabad");
        User kiran = new User("kiran", "Kiran Verma", Arrays.asList("Python", "Data Science"), "Chennai");
        User neha = new User("neha", "Neha Singh", Arrays.asList("Java", "Data Science"), "Bangalore");

        database.put(rahul.id, rahul);
        database.put(priya.id, priya);
        database.put(arjun.id, arjun);
        database.put(sneha.id, sneha);
        database.put(kiran.id, kiran);
        database.put(neha.id, neha);

        // Connections
        addFriendship("rahul", "priya");
        addFriendship("rahul", "arjun");
        addFriendship("rahul", "neha");
        addFriendship("priya", "sneha");
        addFriendship("priya", "kiran");
        addFriendship("arjun", "neha");
        addFriendship("sneha", "kiran");
    }

    private static void addFriendship(String u1, String u2) {
        if (database.containsKey(u1) && database.containsKey(u2)) {
            database.get(u1).friends.add(u2);
            database.get(u2).friends.add(u1);
        }
    }

    /**
     * Serves static resources (HTML/CSS files) from the current folder directory.
     */
    static class FileHandler implements HttpHandler {
        private final String fileName;
        private final String contentType;

        FileHandler(String fileName, String contentType) {
            this.fileName = fileName;
            this.contentType = contentType;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Find file relative to WebServer.class path
            File file = new File(fileName);
            if (!file.exists()) {
                // Try relative to folder location
                String workingDir = System.getProperty("user.dir");
                file = new File(workingDir + File.separator + "social network recommendation" + File.separator + fileName);
            }

            if (!file.exists()) {
                String response = "File Not Found: " + fileName + " in search paths.";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());
            OutputStream os = exchange.getResponseBody();
            Files.copy(file.toPath(), os);
            os.close();
        }
    }

    /**
     * API Handler: Evaluates parameters and returns JSON payload.
     * Recommendation Score = (Mutual Friends * 60) + (Common Skills * 20) + (Same City * 20)
     */
    static class RecommendationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String userId = "";

            if (query != null && query.contains("user=")) {
                userId = query.split("user=")[1].split("&")[0];
            }

            User sourceUser = database.get(userId);
            StringBuilder json = new StringBuilder("[");

            if (sourceUser != null) {
                List<String> recItems = new ArrayList<>();
                
                for (User target : database.values()) {
                    if (target.id.equals(sourceUser.id) || sourceUser.friends.contains(target.id)) {
                        continue;
                    }

                    // 1. Calculate Mutual
                    int mutualCount = 0;
                    for (String f : sourceUser.friends) {
                        if (target.friends.contains(f)) {
                            mutualCount++;
                        }
                    }

                    // 2. Common Skills
                    List<String> commonSkills = new ArrayList<>();
                    for (String s : sourceUser.skills) {
                        if (target.skills.contains(s)) {
                            commonSkills.add(s);
                        }
                    }

                    // 3. Same City
                    boolean sameCity = sourceUser.city.equalsIgnoreCase(target.city);

                    // Compute total score
                    int score = (mutualCount * 60) + (commonSkills.size() * 20) + (sameCity ? 20 : 0);

                    // Format JSON item
                    String skillStr = commonSkills.isEmpty() ? "None" : String.join(", ", commonSkills);
                    String item = String.format(
                        "{\"name\":\"%s\", \"mutualCount\":%d, \"commonSkills\":\"%s\", \"sameCity\":%b, \"score\":%d}",
                        target.name, mutualCount, skillStr, sameCity, score
                    );
                    recItems.add(item);
                }

                // Sort items by score descending
                recItems.sort((a, b) -> {
                    int scoreA = Integer.parseInt(a.split("\"score\":")[1].replace("}", "").trim());
                    int scoreB = Integer.parseInt(b.split("\"score\":")[1].replace("}", "").trim());
                    return Integer.compare(scoreB, scoreA);
                });

                json.append(String.join(",", recItems));
            }

            json.append("]");

            byte[] responseBytes = json.toString().getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
}
