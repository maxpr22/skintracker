package com.skincaretracker.model;

import javafx.beans.property.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalTime;

public class User {
    private final LongProperty id;
    private final StringProperty username;
    private final StringProperty email;
    private final StringProperty password; // Hashed password
    private final StringProperty skinType;
    private final ObjectProperty<LocalTime> preferredReminderTime;
    private final List<String> skinConcerns;
    private final BooleanProperty emailNotifications;
    private final BooleanProperty pushNotifications;
    private final BooleanProperty reminderNotifications;
    private final List<Product> products;
    private final List<Reminder> reminders;

    public User(Long id, String username, String email, String password) {
        this.id = new SimpleLongProperty(id);
        this.username = new SimpleStringProperty(username);
        this.email = new SimpleStringProperty(email);
        this.password = new SimpleStringProperty(password);
        this.skinType = new SimpleStringProperty("");
        this.preferredReminderTime = new SimpleObjectProperty<>(LocalTime.of(9, 0));
        this.skinConcerns = new ArrayList<>();
        this.emailNotifications = new SimpleBooleanProperty(true);
        this.pushNotifications = new SimpleBooleanProperty(true);
        this.reminderNotifications = new SimpleBooleanProperty(true);
        this.products = new ArrayList<>();
        this.reminders = new ArrayList<>();
    }

    // ID
    public Long getId() {
        return id.get();
    }

    public LongProperty idProperty() {
        return id;
    }

    // Username
    public String getUsername() {
        return username.get();
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public StringProperty usernameProperty() {
        return username;
    }

    // Email
    public String getEmail() {
        return email.get();
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public StringProperty emailProperty() {
        return email;
    }

    // Password
    public String getPassword() {
        return password.get();
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    public StringProperty passwordProperty() {
        return password;
    }

    // Skin Type
    public String getSkinType() {
        return skinType.get();
    }

    public void setSkinType(String skinType) {
        this.skinType.set(skinType);
    }

    public StringProperty skinTypeProperty() {
        return skinType;
    }

    // Preferred Reminder Time
    public LocalTime getPreferredReminderTime() {
        return preferredReminderTime.get();
    }

    public void setPreferredReminderTime(LocalTime time) {
        this.preferredReminderTime.set(time);
    }

    public ObjectProperty<LocalTime> preferredReminderTimeProperty() {
        return preferredReminderTime;
    }

    // Skin Concerns
    public List<String> getSkinConcerns() {
        return new ArrayList<>(skinConcerns);
    }

    public void setSkinConcerns(List<String> concerns) {
        this.skinConcerns.clear();
        this.skinConcerns.addAll(concerns);
    }

    public void addSkinConcern(String concern) {
        if (!skinConcerns.contains(concern)) {
            skinConcerns.add(concern);
        }
    }

    public void removeSkinConcern(String concern) {
        skinConcerns.remove(concern);
    }

    // Notification Preferences
    public boolean isEmailNotificationsEnabled() {
        return emailNotifications.get();
    }

    public void setEmailNotifications(boolean enabled) {
        this.emailNotifications.set(enabled);
    }

    public BooleanProperty emailNotificationsProperty() {
        return emailNotifications;
    }

    public boolean isPushNotificationsEnabled() {
        return pushNotifications.get();
    }

    public void setPushNotifications(boolean enabled) {
        this.pushNotifications.set(enabled);
    }

    public BooleanProperty pushNotificationsProperty() {
        return pushNotifications;
    }

    public boolean isReminderNotificationsEnabled() {
        return reminderNotifications.get();
    }

    public void setReminderNotifications(boolean enabled) {
        this.reminderNotifications.set(enabled);
    }

    public BooleanProperty reminderNotificationsProperty() {
        return reminderNotifications;
    }

    // Products
    public List<Product> getProducts() {
        return new ArrayList<>(products);
    }

    public void addProduct(Product product) {
        products.add(product);
    }

    public void removeProduct(Product product) {
        products.remove(product);
    }

    // Reminders
    public List<Reminder> getReminders() {
        return new ArrayList<>(reminders);
    }

    public void addReminder(Reminder reminder) {
        reminders.add(reminder);
    }

    public void removeReminder(Reminder reminder) {
        reminders.remove(reminder);
    }

    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', email='%s', skinType='%s'}",
                getId(), getUsername(), getEmail(), getSkinType());
    }
}
