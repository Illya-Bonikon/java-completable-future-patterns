import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Завдання 2: Демонстрація allOf() та thenCombine()
 * Паралельне отримання та порівняння погодних даних.
 */
public class WeatherComparator {

    // Клас для зберігання погодних даних
    private static class WeatherData {
        String city;
        int temperature; // Температура в °C
        int humidity;    // Вологість у %
        double windSpeed; // Швидкість вітру в м/с

        public WeatherData(String city, int temp, int hum, double wind) {
            this.city = city;
            this.temperature = temp;
            this.humidity = hum;
            this.windSpeed = wind;
        }

        @Override
        public String toString() {
            return String.format("%s: Temp=%d°C, Humidity=%d%%, Wind=%.1f м/с",
                    city, temperature, humidity, windSpeed);
        }
    }

    // Імітація асинхронного отримання погодних даних
    private static CompletableFuture<WeatherData> fetchWeatherAsync(String city, int temp, int hum, double wind) {
        System.out.printf("   [Початок] Отримання даних для %s...%n", city);
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Імітація різної затримки
                TimeUnit.SECONDS.sleep(1 + (city.length() % 2));
                // Імітуємо виняток для одного міста (щоб продемонструвати обробку)
                if (city.equals("Лондон")) {
                    throw new RuntimeException("API error for London");
                }
                return new WeatherData(city, temp, hum, wind);
            } catch (Exception e) {
                // handle() або exceptionally() тут перехопить виняток,
                // і дозволить ланцюжку allOf() продовжитися.
                System.err.printf("   [Помилка] Обробка винятку для %s: %s%n", city, e.getMessage());
                return new WeatherData(city, -100, 100, 99.9); // Повертаємо "погані" дані для ідентифікації помилки
            }
        });
    }
    
    // Приватний метод для аналізу даних та надання висновку
    private static String analyzeWeather(List<WeatherData> weatherList) {
        StringBuilder analysis = new StringBuilder("### Аналіз погодних даних ###\n");
        String bestBeachCity = null;
        String warmestCity = null;
        int maxTemp = Integer.MIN_VALUE;
        
        for (WeatherData data : weatherList) {
            if (data.temperature > maxTemp) {
                maxTemp = data.temperature;
                warmestCity = data.city;
            }
            // Умова для пляжу: тепло (>25C), низька вологість (<70%), слабкий вітер (<5 м/с)
            if (data.temperature > 25 && data.humidity < 70 && data.windSpeed < 5.0) {
                if (bestBeachCity == null) {
                    bestBeachCity = data.city;
                }
            }
        }

        if (bestBeachCity != null) {
            analysis.append("⛱️ **Висновки для пляжу:** Найкраще місце для пляжу - **").append(bestBeachCity).append("**.\n");
        } else {
            analysis.append("😔 **Висновки для пляжу:** Сьогодні пляжний відпочинок не рекомендований. Немає ідеальних умов.\n");
        }

        analysis.append("🧥 **Висновки для тепла:** Найтепліше зараз у місті **").append(warmestCity).append("** (").append(maxTemp).append("°C).\n");
        analysis.append("   У решті міст варто вдягнутись тепліше.\n");
        
        return analysis.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("### Завдання 2: Паралельне об'єднання allOf() ###");
        
        // Створення трьох незалежних асинхронних завдань
        CompletableFuture<WeatherData> kyivFuture = fetchWeatherAsync("Київ", 15, 60, 4.2);
        CompletableFuture<WeatherData> odessaFuture = fetchWeatherAsync("Одеса", 28, 55, 3.0);
        CompletableFuture<WeatherData> londonFuture = fetchWeatherAsync("Лондон", 10, 80, 7.5);

        // Об'єднуємо всі Future за допомогою allOf(). Він повертає CompletableFuture<Void>
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(kyivFuture, odessaFuture, londonFuture);

        // Використовуємо thenApply() після allOf() для обробки результатів
        CompletableFuture<String> analysisFuture = allFutures.thenApply(v -> {
            System.out.println("\n[Об'єднання] Усі асинхронні завдання завершено.");
            
            // Збираємо результати в List<WeatherData>. getNow() безпечно отримує результат,
            // оскільки ми вже знаємо, що всі Future завершилися (завдяки allOf()).
            List<WeatherData> weatherList = Arrays.asList(kyivFuture, odessaFuture, londonFuture)
                .stream()
                .map(future -> future.getNow(new WeatherData("Unknown", 0, 0, 0))) // Використовуємо getNow()
                .collect(Collectors.toList());
            
            return analyzeWeather(weatherList);
        });

        // Блокуємо та отримуємо кінцевий результат
        String finalAnalysis = analysisFuture.get();
        System.out.println("\n==============================================");
        System.out.println(finalAnalysis);
        System.out.println("==============================================");

        // Демонстрація anyOf(): використовуємо для виявлення, яке завдання завершиться першим
        System.out.println("\n### Демонстрація anyOf() (Перший результат) ###");
        CompletableFuture<Object> firstToFinish = CompletableFuture.anyOf(kyivFuture, odessaFuture, londonFuture);
        System.out.printf("⚡ Перший результат, що завершився, був: %s%n", firstToFinish.get());

    }
}