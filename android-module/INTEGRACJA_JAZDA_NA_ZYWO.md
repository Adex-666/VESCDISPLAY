# Jazda na żywo — pliki do Android Studio

Moduł Kotlin + Jetpack Compose do dodania do istniejącej aplikacji Android. Przejazdy są trwale zapisywane lokalnie w SQLite. Nie zastępuje ekranu RT Data ani istniejącej komunikacji BLE.

Wybrałem Kotlin i Jetpack Compose, bo są natywnym wyborem dla Androida i pozwalają wpiąć nowy ekran do istniejącej aplikacji bez przepisywania warstwy BLE.

## Pliki

- `app/src/main/java/pl/replit/jazdanazywo/RideModels.kt` — modele danych.
- `app/src/main/java/pl/replit/jazdanazywo/RideRepository.kt` — SQLite i ustawienie pojemności baterii.
- `app/src/main/java/pl/replit/jazdanazywo/RideAnalytics.kt` — średnie Wh/km, porównanie i rekomendacja.
- `app/src/main/java/pl/replit/jazdanazywo/RideLiveViewModel.kt` — zapis próbek i adapter do RT Data.
- `app/src/main/java/pl/replit/jazdanazywo/RideLiveScreen.kt` — nowy ekran PL, ciemny motyw i wykresy.
- `app/src/test/java/pl/replit/jazdanazywo/RideAnalyticsTest.kt` — testy jednostkowe obliczeń i małej ilości danych.

## Wpięcie

1. Rozpakuj ZIP w katalogu głównym istniejącego projektu. Ścieżki `app/src/...` są już zachowane. W razie potrzeby zmień nazwę pakietu w plikach na pakiet aplikacji.
2. Korzystaj z istniejących bibliotek Compose/Material 3/lifecycle. Jeśli ich brakuje, do `app/build.gradle.kts` dodaj wersje zgodne z projektem, np.:

   ```kotlin
   implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
   implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
   implementation(platform("androidx.compose:compose-bom:2024.12.01"))
   implementation("androidx.compose.material3:material3")
   testImplementation("junit:junit:4.13.2")
   ```

   Nie dodawaj duplikatów, jeśli aplikacja już ma te zależności. SQLite jest w Androidzie, więc biblioteka Room/KSP nie jest potrzebna.
3. W istniejącym komponencie nawigacji utwórz repozytorium/ViewModel i dodaj ekran jako nową trasę:

   ```kotlin
   val context = LocalContext.current
   val repository = remember { RideRepository(context) }
   val rideViewModel: RideLiveViewModel = viewModel(
       factory = remember(repository) { RideLiveViewModelFactory(repository) }
   )
   RideLiveScreen(viewModel = rideViewModel)
   ```

4. W dotychczasowym adapterze BLE przekaż już zdekodowane wartości RT Data:

   ```kotlin
   rideViewModel.setConnectionState(bleIsConnected)
   rideViewModel.onActiveProfileChanged(actualActiveProfile) // potwierdzony profil 1–5
   rideViewModel.onTelemetry(
       RideTelemetry(
           speedKmh = rtData.speedKmh,
           batteryPercent = rtData.batteryPercent,
           consumptionWhPerKm = rtData.consumptionWhPerKm,
       )
   )
   ```

   Profil podawaj po odczycie/potwierdzeniu go przez sterownik, nie tylko po tapnięciu. Po utracie połączenia wywołaj `setConnectionState(false)`. Przekazuj `null` dla nieznanych lub niedostępnych pomiarów — aplikacja nie wymyśla wartości. Jeśli RT Data pochodzi z Flow, wywołuj te metody w istniejącym collectorze.
5. Wpisz na ekranie użyteczną pojemność baterii w Wh, aby pokazać przybliżony zasięg. Bez niej zapis i porównanie Wh/km nadal działają, ale wykres zasięgu pozostaje pusty.

## Założenia

- Próbka jest zapisywana co ok. 1 s; zmiana profilu wywołuje zapis od razu, jeżeli są dostępne odczyty.
- Wh/km jest ważone odległością wyznaczoną z kolejnych prędkości. Przerwy ponad 5 s i brakujące dane nie są łączone.
- Rekomendacja wymaga min. 30 poprawnych próbek oraz min. 0,5 km dla profilu. W uzasadnieniu pokazuje zmierzoną średnią prędkość i jej odchylenie standardowe; profili bez danych wystarczających nie rekomenduje.
- Zasięg używa ostatniego zapisanego SOC i wpisanej pojemności użytkowej. Liniowe SOC→energia to przybliżenie, nie gwarancja; nie uwzględnia rezerwy BMS, temperatury ani degradacji.
- Nie zakładamy szczegółów protokołu VESC ani BLE characteristics. Podłącz do obecnego dekodera RT Data. Jeśli miernik Wh/km w aplikacji jest wspólną średnią całej jazdy, a nie wartością właściwą dla aktualnego profilu, nie używaj go do porównania profili bez dostarczenia poprawnej miary.

## Manifest i testy

`AndroidManifest.xml` nie wymaga zmian — moduł nie dodaje aktywności, usługi ani uprawnień BLE. Testy uruchom w Android Studio przez `app > Tasks > verification > testDebugUnitTest` lub `./gradlew :app:testDebugUnitTest`.
