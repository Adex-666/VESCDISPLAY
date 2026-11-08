# VESC Display — Android Studio

To samodzielny projekt Android Studio dla trybu „Jazda na żywo”, oparty na Kotlinie i Jetpack Compose. Projekt ma konfigurację Gradle, aktywność startową, manifest, Gradle Wrapper oraz testy jednostkowe.

## Otwórz i uruchom

- W Android Studio wybierz Open i wskaż ten folder android-module.
- Wybierz JDK 17, poczekaj na synchronizację Gradle i uruchom konfigurację app na emulatorze albo urządzeniu Android.
- Testy logiki możesz uruchomić poleceniem ./gradlew :app:testDebugUnitTest na macOS/Linux albo gradlew.bat :app:testDebugUnitTest w Windows.

Ekran otwiera się w trybie offline: pokazuje brak połączenia i nie tworzy przykładowych odczytów. Repozytorium nie zawiera kodu istniejącego sterownika BLE ani dekodera RT Data, więc dane na żywo pojawią się dopiero po podłączeniu adaptera opisanego w INTEGRACJA_JAZDA_NA_ZYWO.md. Aplikacja nie zgaduje charakterystyk BLE ani formatu VESC.

## Co zawiera

- lokalny zapis próbek i przejazdów w SQLite;
- historię offline i wykresy pięciu profili;
- średnie zużycie Wh/km i rekomendację wyliczaną tylko z wystarczających danych;
- szacowany zasięg po wpisaniu użytecznej pojemności baterii;
- kolory, etykiety i komunikaty w języku polskim.

Wartości zasięgu są przybliżeniem i zależą od poprawnych danych RT Data oraz wpisanej pojemności użytkowej baterii.
