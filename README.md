# Skin Care Tracker

A JavaFX application for tracking your skincare routine, products, and reminders.

## Features

- User authentication and profile management
- Product tracking with ratings and allergic reactions
- Reminder system for skincare routines
- Profile customization with skin type and concerns
- Modern, user-friendly interface

## Requirements

- Java 17 or higher
- Maven

## Building and Running

1. Clone the repository
2. Navigate to the project directory
3. Build the project:
   ```bash
   mvn clean package
   ```
4. Run the application:
   ```bash
   mvn javafx:run
   ```

## Default Login

For testing purposes, use these credentials:
- Username: demo
- Password: password

## Features Overview

### Products Management
- Add and track skincare products
- Rate products
- Mark products as allergic/non-allergic
- Add product descriptions and notes

### Reminders
- Set reminders for product usage
- Customize reminder frequency
- Mark reminders as completed
- Filter reminders by date and status

### Profile Management
- Update personal information
- Set skin type and concerns
- Manage notification preferences
- Change password
- Delete account

## Database

The application uses SQLite for data persistence. The database file `skincare_tracker.db` will be created automatically in the project root directory when the application is first run.
