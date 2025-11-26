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
    git clone https://github.com/your-username/gas_prices.git
    cd gas_prices
    ```
2.  **Otvorte v Android Studio:** Otvorte projekt v Android Studio.
3.  **Synchronizujte Gradle:** Android Studio by mal automaticky vyzvať na synchronizáciu Gradle. Ak nie, kliknite na `File > Sync Project with Gradle Files`.
4.  **Spustite aplikáciu:** Spustite aplikáciu na emulátore alebo fyzickom zariadení.
