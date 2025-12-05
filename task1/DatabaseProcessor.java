import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DatabaseProcessor {

    private static final String USER_NAME = "Olexandr_K";

    private static CompletableFuture<Long> fetchUserIdAsync(String userName) {
        System.out.printf("[%s] 1. Асинхронний запит ID для користувача '%s' (Затримка 2с)...%n", 
            Thread.currentThread().getName(), userName);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(2); 
                long userId = 100500L; 
                System.out.printf("[%s]    --> Завдання 1: ID отримано: %d.%n", 
                    Thread.currentThread().getName(), userId);
                return userId;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Потік перервано", e);
            }
        });
    }

    private static CompletableFuture<String> fetchUserProfileAsync(long userId) {
        System.out.printf("[%s] 2. Асинхронний запит профілю за ID: %d (Затримка 1с)...%n", 
            Thread.currentThread().getName(), userId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                String profileData = String.format("ID: %d, Ім'я: %s, Місто: Харків, Статус: Активний", 
                    userId, USER_NAME);
                
                System.out.printf("[%s]    --> Завдання 2: Профіль оброблено.%n", 
                    Thread.currentThread().getName());
                return profileData; 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Потік перервано", e);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        System.out.println("### Завдання 1: Ланцюжок з thenCompose() ###");
        System.out.printf("Головний потік: %s%n%n", Thread.currentThread().getName());

        CompletableFuture<String> fullProfileFuture = fetchUserIdAsync(USER_NAME)
            .thenCompose(DatabaseProcessor::fetchUserProfileAsync)
            
            .exceptionally(ex -> {
                System.err.printf("\n[ERROR] !!! Помилка в ланцюжку: %s%n", ex.getCause().getMessage());
                return "Помилка при отриманні даних користувача. Повернено значення за замовчуванням."; 
            });

        String finalResult = fullProfileFuture.get();
        
        System.out.println("\n========================================================");
        System.out.println("Кінцевий результат: " + finalResult);
        System.out.println("========================================================");
    }
}