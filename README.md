# DriveShare - Peer-to-Peer Car Rental Platform

CIS 476 Software Architecture and Design Patterns - Term Project (Fall 2026)
Amal Hammoud (individual project)

DriveShare connects car owners with people who want to rent a car for a short time, similar to Turo.
It is a Java Swing desktop application with an SQLite database.

## Contents of this repository

| Path | What it is |
|---|---|
| `src/driveshare/` | Java source code (thoroughly commented) |
| `lib/sqlite-jdbc-3.46.1.3.jar` | SQLite JDBC driver (the only external library) |
| `docs/Report.docx` | Report: class diagrams and descriptions, database schema and descriptions, UI screenshots |
| `docs/diagrams/` | Class diagrams and database diagram (PNG + PlantUML sources) |
| `docs/screenshots/` | User-interface screenshots |
| `slides/DriveShare_Presentation.pptx` | Presentation slides used in the video |
| `run.bat`, `run.sh` | Compile and run |

## How to run

Requires Java 11 or newer (JDK, so `javac` is available).

- Windows: double-click `run.bat`, or run it from a terminal in this folder
- macOS / Linux: `./run.sh` (run `chmod +x run.sh` once if needed)

Manual commands (macOS/Linux; on Windows replace `:` with `;` in the classpath):

```
javac -d out -cp lib/sqlite-jdbc-3.46.1.3.jar $(find src -name "*.java")
java -cp out:lib/sqlite-jdbc-3.46.1.3.jar driveshare.Main
```

The database file `driveshare.db` is created on the first run, with two demo accounts and four cars:

| Role | Email | Password |
|---|---|---|
| Renter | adam@driveshare.com | renter123 |
| Owner | sara@driveshare.com | owner123 |

Security answers for both demo accounts: `Dearborn`, `Max`, `Pizza`.
Delete `driveshare.db` to start over with fresh demo data.

## Features

- Registration and login with email and password; three security questions set at registration
- Owners list cars (model, year, mileage, availability window, pick-up location, price per day, optional details), update price and availability, pause or resume a listing, and view an availability calendar
- The same car can never be rented for overlapping dates (checked in code and by a database trigger)
- Renters search by location, dates, maximum price and minimum seats
- Renters watch cars and are notified when the car becomes available or the price drops to their limit
- Booking requests that owners confirm or decline; renters can cancel
- Messaging between owners and renters
- In-app notifications for booking requests, confirmations, payments, watch alerts and new messages
- Simulated payment: a "Pay $X" button moves the amount from the renter's balance to the owner's and notifies both

Rental History and Reviews were optional for individual projects and are not included.

## Design patterns

| Pattern | Purpose | Classes |
|---|---|---|
| Singleton | User session | `auth.SessionManager` |
| Observer | Notify renters when a watched car meets their conditions | `observer.CarSubject`, `CarWatchNotifier`, `CarObserver`, `WatchObserver` |
| Mediator | Communication between UI screens | `ui.mediator.UIMediator`, `DriveShareMediator`, `UIComponent` + all panels |
| Builder | Build car listings with optional attributes | `model.CarListingBuilder` -> `model.Car` |
| Proxy | Secure access to the simulated payment system | `payment.PaymentService`, `PaymentProxy`, `RealPaymentService` |
| Chain of Responsibility | Password recovery with three security questions | `recovery.RecoveryHandler`, `SecurityQuestionHandler`, `PasswordRecoveryService` |

Passwords and security answers are stored only as salted PBKDF2-HMAC-SHA256 hashes.

## Source layout

```
src/driveshare/
  Main.java              start-up
  auth/                  SessionManager (Singleton), AuthService, PasswordHasher
  recovery/              Chain of Responsibility for password recovery
  observer/              Observer for watched cars
  payment/               Proxy for payments
  model/                 User, Car, CarListingBuilder (Builder), Booking, Message, Notification, Watch, ...
  service/               CarService, BookingService, WatchService, MessageService, NotificationService, Services
  db/                    Database (schema), DAOs, demo data
  ui/                    MainFrame, screens and dialogs
  ui/mediator/           Mediator interface, concrete mediator, UIComponent, UIEvent
```
