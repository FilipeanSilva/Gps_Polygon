# **Polygon Game – Android Application**

## **About the Project**
This project was developed as part of the **Mobile Architectures** course during the **2020/2021** academic year. The main goal was to create an **Android game** called **Polygon**, implementing **client-server communication** and cloud-based data storage.

## **Features**
- **Multiplayer Communication**
  - Uses **TCP sockets** for initial communication between server and clients.
  - Once the game starts, communication is handled via **Google Firebase Cloud**.
- **Cloud Data Storage**
  - Player data and game-related information are stored in **Firebase**.
- **User Interface**
  - Developed using **Android XML layouts**.

## **Technical Stack**
- **Programming Language**: Java (Android Development)
- **Networking**: TCP sockets for server-client communication
- **Cloud Storage**: Google Firebase Cloud
- **Android Components**: Activities, XML Layouts

## **File Structure**
The project includes the following key files:

| **File**          | **Description** |
|------------------|---------------|
| `GameActivity.java` | Handles the game logic and execution. |
| `MainActivity.java` | Manages the game startup and user interaction. |
| `Mensagem.java` | Defines the data structure for TCP communication. |
| `activity_game.xml` | UI layout for the game screen. |
| `activity_main.xml` | UI layout for the initial game screen. |

## **How to Run**
1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/PolygonGame.git
   cd PolygonGame
   ```
2. **Open in Android Studio**
   - Open Android Studio and import the project.
   - Ensure Firebase is correctly configured.
3. **Run the App**
   - Connect an Android device or use an emulator.
   - Click **Run** (`Shift + F10` in Android Studio).

## **User Interface**
The game includes:
- A **main menu** where users can start the game.
- A **game screen** where interactions happen in real time.

## **Final Thoughts**
This project provided valuable experience in **Android development, real-time networking, and cloud integration**. The challenges of **communication handling and data synchronization** helped reinforce key software engineering principles.
