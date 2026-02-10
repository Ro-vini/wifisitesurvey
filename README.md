# WiFi Site Survey Application
The WiFi Site Survey Application is a comprehensive tool designed to help users conduct WiFi site surveys, analyze WiFi signals, and manage survey data. The application provides a user-friendly interface for users to conduct surveys, view results, and interact with the application's core functionality. The application is built using a combination of Java, AndroidX libraries, and Google Maps Android API.

[The original Article, with technology, fundamentals, diagrams and test, can be verified here](./Article.pdf).

## Features
- Conduct WiFi site surveys and analyze WiFi signals
- Display survey results and provide location visualization using Google Maps
- Manage survey data and track location updates
- Record data points with location information
- Retrieve data points for a specific survey
- Toggle location tracking on and off
- Classify signal strength into categories like "Excelente", "Bom", "Razoável", "Fraco", or "Sem Sinal"
- Calculate signal quality based on the RSSI value
- Map WiFi standard of a scan result to a human-readable string

## Tech Stack
- Java
- AndroidX libraries
- Google Maps Android API
- Room persistence library
- AndroidX lifecycle
- AndroidX room
- MPAndroidChart

## Installation
To install the application, follow these steps:
1. Clone the repository using `git clone`
2. Open the project in Android Studio
3. Build the project using `gradle build`
4. Run the application on a physical device or emulator

## Usage
To use the application, follow these steps:
1. Launch the application on a physical device or emulator
2. Conduct a WiFi site survey by navigating to the survey activity
3. View survey results and location visualization using Google Maps
4. Manage survey data and track location updates
5. Record data points with location information
6. Retrieve data points for a specific survey
7. Toggle location tracking on and off

## Project Structure

<img width="943" height="913" alt="image" src="https://github.com/user-attachments/assets/d471e172-7cda-4889-9ff7-431ea392ddfb" />

```bash
app/src/main/java/com/example/wifisitesurvey
├── data
│   ├── database    # Room's Settings (AppDatabase)
│   ├── model       # Entities (Survey, DataPoints)
│   └── repository  # SurveyRepository (Data management)
├── services        # WifiService and LocationProvider (Access to Hardware)
├── ui
│   ├── main        # Main screen
│   └── survey      # SurveyActivity and SurveyViewModel (Maps logic)
└── utils           # Analysis algoritms and constants

```

## Screenshots & Videos

https://github.com/user-attachments/assets/95b00b96-8ebe-405b-bf8f-50ac16660c8d

https://github.com/user-attachments/assets/33bea2db-68c4-4be0-9ef5-9ef1d9766313

## Future Implementations

The project is constantly evolving to overcome the limitations of indoor mapping:

* [ ] **Migration to OpenStreetMap (osmdroid):** To reduce proprietary dependencies and enable improved offline functionality.


* [ ] **LiDAR/SLAM Integration:** Use of sensors for real-time architectural mapping of the environment (Google Cartographer).


* [ ] **Dedicated Hardware:** Future integration with Raspberry Pi + Kali Linux for in-depth packet and jitter analysis.

## Contributing
To contribute to the project, please follow these steps:
1. Fork the repository
2. Create a new branch
3. Make changes and commit them
4. Create a pull request

## Contact
For any questions or concerns, please contact us at:
* **Nícolas Ruzza** - [nic.ruzza@gmail.com](mailto:nic.ruzza@gmail.com)

* **Vinícius Eduardo Roveri** - [viroveri117@gmail.com](mailto:viroveri117@gmail.com)

## Thanks Message
We would like to thank all the contributors and users of the WiFi Site Survey Application.
