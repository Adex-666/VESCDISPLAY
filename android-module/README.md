# Jazda na żywo — moduł Android

Dodatkowy ekran do istniejącej aplikacji Android w Kotlinie i Jetpack Compose. Zapisuje przejazdy lokalnie, pokazuje odczyty RT Data, porównuje pięć profili prędkości pod względem Wh/km i szacowanego zasięgu oraz podaje rekomendację wyłącznie na podstawie pomiarów.

> To repozytorium zawiera gotowy moduł do wpięcia, a nie samodzielną aplikację: kod istniejącego szkieletu i jego dekodera BLE nie był dołączony. Adapter BLE musi przekazywać do `RideLiveViewModel` wartości już odczytane i zdekodowane przez aplikację.

Wybrano Kotlin + Jetpack Compose, bo pasują do natywnej aplikacji Android i umożliwiają dodanie ekranu bez przepisywania istniejącej warstwy BLE.

## Zawartość

- `app/src/main/java/pl/replit/jazdanazywo/RideLiveScreen.kt` — ekran po polsku, ciemny motyw, historia i wykresy.
- `app/src/main/java/pl/replit/jazdanazywo/RideLiveViewModel.kt` — punkty integracji z istniejącym stanem połączenia, profilem i RT Data.
- `app/src/main/java/pl/replit/jazdanazywo/RideRepository.kt` — trwały zapis danych i pojemności baterii w SQLite.
- `app/src/main/java/pl/replit/jazdanazywo/RideAnalytics.kt` — obliczenia profili, zużycia, zasięgu i rekomendacji.
- `app/src/main/java/pl/replit/jazdanazywo/RideModels.kt` — modele danych.
- `app/src/test/java/pl/replit/jazdanazywo/RideAnalyticsTest.kt` — testy logiki porównania.
- `INTEGRACJA_JAZDA_NA_ZYWO.md` — szczegółowa instrukcja wpięcia i zależności.

## Najważniejsze założenia

- Próbki są zapisywane lokalnie; zmianę profilu zapisuje osobna próbka.
- Gdy aplikacja jest offline, ekran pokazuje zapisane przejazdy, a wartości na żywo pozostają puste.
- Brak danych nie jest zastępowany wartościami przykładowymi.
- Rekomendacja wymaga min. 30 poprawnych próbek i 0,5 km pomiaru dla profilu.
- Zasięg wymaga wpisania użytkowej pojemności baterii. Wyliczenie SOC na energię jest przybliżeniem.
- Moduł nie zgaduje formatu komunikatów VESC ani BLE characteristics.

## Dodanie do istniejącej aplikacji

1. Skopiuj folder `app/src` do projektu Android Studio.
2. Sprawdź wymagane biblioteki Gradle i przykład utworzenia ViewModelu w `INTEGRACJA_JAZDA_NA_ZYWO.md`.
3. Dodaj `RideLiveScreen` do nawigacji i połącz trzy callbacki: `setConnectionState`, `onActiveProfileChanged`, `onTelemetry`.
4. Uruchom testy w projekcie: `./gradlew :app:testDebugUnitTest`.

`AndroidManifest.xml` nie wymaga zmian. Pliki nie zawierają `.git` ani licencji — można utworzyć z nich nowe repozytorium, a licencję dodać zgodnie z decyzją właściciela aplikacji.
