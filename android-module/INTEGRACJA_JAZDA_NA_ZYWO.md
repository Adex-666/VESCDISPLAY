# Integracja trybu „Jazda na żywo”

## Projekt Android Studio

Folder android-module jest samodzielnym projektem Kotlin/Jetpack Compose. Otwórz go w Android Studio, używając JDK 17. Gradle Wrapper pobiera Gradle 8.11.1, a Gradle pobiera wymagane zależności. Główna aktywność uruchamia RideLiveScreen. Testy uruchom poleceniem ./gradlew :app:testDebugUnitTest (macOS/Linux) albo gradlew.bat :app:testDebugUnitTest (Windows).

## Granica integracji BLE

Repozytorium nie zawiera istniejącego szkieletu aplikacji ani kodu BLE/dekodera VESC. Samodzielna aplikacja uruchamia więc ekran w trybie offline: nie udaje połączenia, nie pokazuje przykładowych wartości i nie wysyła poleceń do sterownika. Nie dodano uprawnień BLE, ponieważ brak rzeczywistej warstwy BLE do podłączenia. Zmiana profilu pozostaje tylko wyświetlana przez nowy ekran dopiero po przekazaniu potwierdzonego profilu z istniejącej aplikacji.

W swojej aplikacji wywołuj metody RideLiveViewModel z już istniejącego źródła danych:

- setConnectionState(true/false) po zmianie stanu połączenia;
- onActiveProfileChanged(numer) po odczytaniu/potwierdzeniu profilu 1–5 ze sterownika;
- onTelemetry(RideTelemetry(...)) po otrzymaniu zdekodowanych pomiarów speedKmh, batteryPercent i consumptionWhPerKm.

Dla wartości nieznanych przekaż null. Nie łącz ekranu z niezweryfikowanymi bajtami ani charakterystykami BLE. Jeśli RT Data pokazuje wspólną średnią Wh/km dla całej jazdy, a nie wartość dla bieżącego profilu, nie używaj jej do porównywania profili bez wiarygodnego pomiaru energii przypisanego do profilu.

## Obliczenia i założenia

- Próbki zapisywane są co około sekundę; potwierdzona zmiana profilu wymusza próbkę.
- Przejazdy i ustawienie pojemności są trwale przechowywane lokalnie w SQLite.
- Odcinki z przerwą pomiaru ponad 5 sekund są pomijane; nie łączymy jazdy przez przerwę ani zmianę profilu.
- Porównanie/rekomendacja wymaga co najmniej 30 poprawnych próbek oraz 0,5 km pomiaru dla profilu. Profile bez tych danych są jawnie oznaczone jako niewystarczające.
- Szacowany zasięg używa ostatniego zapisanego poziomu baterii i podanej pojemności użytkowej. Liniowe przeliczenie procentu baterii na energię jest przybliżeniem, nie gwarancją.
- Manifest nie wymaga zmian dla samego ekranu. Prawdziwe uprawnienia i cykl życia BLE należy zachować w istniejącej aplikacji; nie są częścią tego projektu.
