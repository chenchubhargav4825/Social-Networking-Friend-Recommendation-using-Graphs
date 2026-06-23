import java.util.*;

public class ConnectSphere {

    public static class User {
        String id;
        String name;
        List<String> skills;
        String city;
        List<String> friends;

        public User(String id, String name, List<String> skills, String city) {
            this.id = id;
            this.name = name;
            this.skills = skills;
            this.city = city;
            this.friends = new ArrayList<>();
        }
    }

    private static Map<String, User> database = new HashMap<>();

    public static void main(String[] args) {
        initializeDatabase();
        
        System.out.println("--- ConnectSphere Java Recommendation Engine ---");
        String testUser = "rahul";
        User source = database.get(testUser);
        
        System.out.println("Recommendations for: " + source.name);
        
        for (User target : database.values()) {
            if (target.id.equals(source.id) || source.friends.contains(target.id)) {
                continue;
            }
            
            // 1. Mutual Friends
            int mutualCount = 0;
            for (String f : source.friends) {
                if (target.friends.contains(f)) {
                    mutualCount++;
                }
            }
            
            // 2. Common Skills
            int commonSkills = 0;
            for (String s : source.skills) {
                if (target.skills.contains(s)) {
                    commonSkills++;
                }
            }
            
            // 3. Same City
            boolean sameCity = source.city.equalsIgnoreCase(target.city);
            
            // Score
            int score = (mutualCount * 60) + (commonSkills * 20) + (sameCity ? 20 : 0);
            
            System.out.println("\nUser: " + target.name);
            System.out.println("Mutual Friends: " + mutualCount);
            System.out.println("Common Skills Count: " + commonSkills);
            System.out.println("Same City: " + (sameCity ? "Yes" : "No"));
            System.out.println("Recommendation Score: " + score);
        }
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

        addFriendship("rahul", "priya");
        addFriendship("rahul", "arjun");
        addFriendship("rahul", "neha");
        addFriendship("priya", "sneha");
        addFriendship("priya", "kiran");
        addFriendship("arjun", "neha");
        addFriendship("sneha", "kiran");
    }

    private static void addFriendship(String u1, String u2) {
        database.get(u1).friends.add(u2);
        database.get(u2).friends.add(u1);
    }
}
