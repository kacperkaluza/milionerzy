# Milionerzy Świętokrzyskiego

Ekonomiczna gra planszowa inspirowana Monopoly dla 2–4 graczy. Celem gry jest zgromadzenie największego majątku i doprowadzenie przeciwników do bankructwa poprzez kupno nieruchomości, ulepszanie i pobieranie czynszu.

## Technologie

- **Java 25** - język programowania
- **JavaFX 21.0.6** - framework graficzny
- **Maven** - zarządzanie zależnościami i budowanie projektu
- **FXGL 17.3** - biblioteka do gier
- **JUnit 5** - testy jednostkowe
- **TestFX** - testy interfejsu użytkownika

## Wymagania

- Java Development Kit (JDK) 25 lub nowszy
- Maven 3.6 lub nowszy

## Instalacja i uruchomienie

### Klonowanie repozytorium

```bash
git clone https://github.com/kacperkaluza/milionerzy.git
cd milionerzy
```

### Budowanie projektu

```bash
mvn clean install
```

### Uruchomienie gry

```bash
mvn javafx:run
```

Alternatywnie, na systemach Unix:
```bash
./mvnw javafx:run
```

Na systemach Windows:
```bash
mvnw.cmd javafx:run
```

## Funkcje

### Mechanika gry
- **Plansza i pola** - pełna reprezentacja pól planszy z właściwościami nieruchomości
- **Rzuty kostkami i ruchy** - losowe rzuty kostkami i automatyczne przesuwanie pionków
- **Transakcje** - system kupna nieruchomości, aukcje, płatności czynszu
- **Zarządzanie hipotekami** - możliwość zastawiania i wykupywania nieruchomości
- **Budowanie** - budowa domów i hoteli z interfejsem wyboru rozbudowy
- **Karty Szansa/Kasa Społeczna** - system kart z różnymi akcjami (nagrody, kary, przesunięcia)
- **Więzienie** - mechanika pobytu w więzieniu i sposoby wyjścia

### Interakcje między graczami
- **Aukcje na żywo** - interfejs licytacji nieruchomości między graczami
- **Negocjacje** - system ofert i wymian między graczami (TradeOffer)
- **Płatności między graczami** - automatyczne transfery pieniędzy

### System gry
- **Zapis i odczyt stanu gry** - pełne zapisywanie i wczytywanie stanu rozgrywki (SaveManager)
- **Tryb lokalny** - gra dla wielu graczy na jednym urządzeniu
- **Tryb sieciowy** - wsparcie dla gry przez sieć (NetworkManager, Lobby)
- **Obsługa zdarzeń** - system event-driven (GameEvent, GameEventListener)

### Interfejs użytkownika
- **Menu główne** - elegancki interfejs z animacjami
- **Widok planszy** - czytelne wyświetlanie stanu gry
- **Panel graczy** - wyświetlanie kapitału, posiadanych nieruchomości
- **Okna dialogowe** - interaktywne okna dla aukcji, wymiany, budowania
- **Ustawienia** - konfiguracja gry
- **Ekran autorów** - informacje o twórcach

## Struktura projektu

```
src/main/java/com/kaluzaplotecka/milionerzy/
├── Launcher.java                    # Punkt wejścia aplikacji
├── events/                          # System zdarzeń
│   ├── GameEvent.java
│   └── GameEventListener.java
├── model/                           # Logika gry
│   ├── Auction.java                 # System aukcji
│   ├── Board.java                   # Plansza gry
│   ├── GameState.java               # Stan gry
│   ├── Player.java                  # Gracz
│   ├── SaveManager.java             # Zapis/odczyt gry
│   ├── TradeOffer.java              # Oferty wymiany
│   ├── cards/
│   │   └── EventCard.java           # Karty Szansa/Kasa Społeczna
│   └── tiles/                       # Typy pól
│       ├── Tile.java
│       ├── PropertyTile.java
│       ├── ChanceTile.java
│       └── CommunityChestTile.java
├── network/                         # Gra sieciowa
│   ├── NetworkManager.java
│   ├── GameMessage.java
│   ├── NetworkGameEventListener.java
│   └── PendingMessageTracker.java
└── view/                            # Interfejs użytkownika
    ├── MainMenu.java                # Menu główne
    ├── GameBoardView.java           # Widok planszy
    ├── AuctionView.java             # Interfejs aukcji
    ├── LobbyView.java               # Lobby sieciowe
    ├── LoadGameView.java            # Wczytywanie gry
    ├── SettingsView.java            # Ustawienia
    ├── AuthorsView.java             # Informacje o autorach
    └── NetworkStatusBox.java        # Status połączenia
```

## Testowanie

Projekt zawiera kompleksowy zestaw testów jednostkowych i integracyjnych:

```bash
mvn test
```

Testy obejmują:
- `GameStateTest` - testy logiki gry
- `PlayerTest` - testy funkcjonalności graczy
- `PropertyTileTest` - testy mechaniki nieruchomości
- `AuctionTest` - testy systemu aukcji
- `TradeOfferTest` - testy wymian między graczami
- `SaveManagerTest` - testy zapisu/odczytu gry
- `NetworkGameTest` - testy gry sieciowej
- `MainMenuTest` - testy interfejsu użytkownika

## Tworzenie paczki wykonywalnej

```bash
mvn clean package
```

## Dokumentacja

Szczegółowy opis projektu znajduje się w pliku `design.pdf`.

## Autorzy

Informacje o autorach dostępne są w grze w sekcji "Autorzy" z menu głównego.

## Licencja

Projekt stworzony w celach edukacyjnych.
