# Gas Prices App

Jednoduchá Android aplikácia na sledovanie historických priemerných cien pohonných hmôt na Slovensku. Aplikácia vizualizuje týždenné dáta z oficiálneho zdroja (Štatistický úrad SR) pomocou interaktívneho čiarového grafu a zobrazuje aktuálne trendy s regresnými čiarami.

## Funkcie

*   **Historické ceny palív:** Zobrazuje priemerné týždenné ceny pre Natural 95, Natural 98 a Diesel.
*   **Interaktívny graf:**
    *   Zoomovanie (priblíženie/oddialenie).
    *   Posúvanie (panovanie) grafu.
    *   Zobrazenie hodnoty (ceny) priamo pri každom dátovom bode.
    *   Os X zobrazuje dátumové rozsahy týždňov v chronologickom poradí (najstaršie vľavo, najnovšie vpravo).
*   **Regresné čiary:** Každá krivka má aj regresnú čiaru, ktorá vizuálne znázorňuje cenový trend.
*   **Aktuálne ceny:** V prehľadnej hlavičke aplikácie sa zobrazujú najnovšie dostupné priemerné ceny pre každý typ paliva.

## Zdroj dát

Dáta o cenách palív sú získavané z oficiálneho zdroja:
[Štatistický úrad Slovenskej republiky - Databáza Eurostat (datacube.statistics.sk)](http://datacube.statistics.sk/)

## Nastavenie a spustenie

1.  **Klonujte repozitár:**
    ```bash
    git clone https://github.com/Darxie/gas_prices.git
    cd gas_prices
    ```
2.  **Otvorte v Android Studio:** Otvorte projekt v Android Studio.
3.  **Synchronizujte Gradle:** Android Studio by mal automaticky vyzvať na synchronizáciu Gradle. Ak nie, kliknite na `File > Sync Project with Gradle Files`.
4.  **Spustite aplikáciu:** Spustite aplikáciu na emulátore alebo fyzickom zariadení.

## iOS verzia (Xcode)

V repozitári je pripravená aj iOS implementácia v adresári `ios/`.

1.  **Požiadavky:**
    * Xcode 26+ (na macOS)
    * `xcodegen` (nainštalujte cez `brew install xcodegen`)
2.  **Vygenerujte projekt:**
    ```bash
    cd ios
    xcodegen generate
    ```
3.  **Otvorte projekt v Xcode:**
    ```bash
    open GasPricesiOS.xcodeproj
    ```
4.  **Nastavte podpisovanie (Signing):**
    * V targete `GasPricesiOS` nastavte svoj `Team`.
    * Bundle identifier je predvolene `cz.feldis.gasprices.ios` (môžete ho zmeniť podľa potreby).
5.  **Inštalácia na iPhone cez Xcode:**
    * Pripojte iPhone káblom alebo cez Wi-Fi debugging.
    * Vyberte zariadenie v Xcode a stlačte Run.
    * Pri prvom spustení povoľte dôveryhodnosť developer certifikátu v nastaveniach iPhonu.

### iOS testy

Spustenie testov z terminálu:

```bash
cd ios
xcodebuild -project GasPricesiOS.xcodeproj \
  -scheme GasPricesiOS \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.4' \
  test
```

## Verzovanie buildu

Predvolené verzie sú nastavené v `app/build.gradle.kts`, ale pri release ich vieš prepísať cez Gradle parametre:

```bash
./gradlew :app:assembleRelease -PversionCode=2 -PversionName=1.0.1
```

Rovnaké prepísanie funguje aj pre bundle:

```bash
./gradlew :app:bundleRelease -PversionCode=2 -PversionName=1.0.1
```

## Firebase App Distribution

1.  **Vytvorte Firebase projekt:** Založte projekt v [Firebase Console](https://console.firebase.google.com), pridajte Android aplikáciu s balíkom `cz.feldis.gasprices`, a stiahnite si súbor služby (JSON) do adresára `distribution/`.
2.  **Skopírujte ukážkové konfiguračné súbory:**
    * `cp keystore.properties.sample keystore.properties` a doplňte cestu ku keystore (`storeFile`), heslá a alias.
    * `cp firebase.properties.sample firebase.properties` a doplňte `appId`, `serviceCredentialsFile`, `groups`, `testers` a `releaseNotesFile`. `firebase.properties` aj súbor služby sú ignorované v `.gitignore`.
3.  **Nainštalujte Firebase CLI:** `npm install -g firebase-tools` a prihláste sa cez `firebase login`.
4.  **Zostavte podpisaný bundle:** `./gradlew bundleRelease`.
5.  **Nahrajte na Firebase App Distribution pomocou CLI:** 
      ```bash
      firebase appdistribution:distribute app/build/outputs/bundle/release/app-release.aab \
        --app $(grep appId firebase.properties | cut -d'=' -f2) \
        --groups qa,testers \
        --release-notes distribution/release-notes.md
      ```
6.  **Pozvite testerov:** Uistite sa, že e-mailové adresy uvedené v `firebase.properties` sú pridané ako testeri v Firebase Console alebo v CLI príkaze.
7.  **Zdieľajte link:** Po úspešnom nahratí dostanú pozvaní tester jedením odkaz na aktualizáciu aplikácie bez nutnosti manuálneho zdieľania APK.

## Firebase Crashlytics a Performance Monitoring

Crashlytics a Performance Monitoring sú integrované cez Gradle pluginy a Firebase BoM.

1.  Uistite sa, že `google-services.json` je v module `app/` (`app/google-services.json`).
2.  Spustite appku aspoň raz na zariadení, aby sa inicializoval Firebase SDK.
3.  Pre overenie Crashlytics môžete odoslať test výnimku (napr. v debug režime) a skontrolovať ju vo Firebase Console.
4.  Performance Monitoring začne zbierať automatické metriky (network traces, app start) po prvom spustení.
