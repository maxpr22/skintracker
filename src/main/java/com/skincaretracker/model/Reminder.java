package com.skincaretracker.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Reminder {
    private final LongProperty id;
    private final ObjectProperty<Product> product;
    private final StringProperty message;
    private final ObjectProperty<LocalDateTime> dateTime;
    private final BooleanProperty completed;

    public Reminder(Long id, Product product, String message, LocalDateTime dateTime) {
        this.id = new SimpleLongProperty(id);
        this.product = new SimpleObjectProperty<>(product);
        this.message = new SimpleStringProperty(message);
        this.dateTime = new SimpleObjectProperty<>(dateTime);
        this.completed = new SimpleBooleanProperty(false);
    }

    // ID
    public Long getId() {
        return id.get();
    }

    public LongProperty idProperty() {
        return id;
    }

    // Product
    public Product getProduct() {
        return product.get();
    }

    public void setProduct(Product product) {
        this.product.set(product);
    }

    public ObjectProperty<Product> productProperty() {
        return product;
    }

    // Message
    public String getMessage() {
        return message.get();
    }

    public void setMessage(String message) {
        this.message.set(message);
    }

    public StringProperty messageProperty() {
        return message;
    }

    // DateTime
    public LocalDateTime getDateTime() {
        return dateTime.get();
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime.set(dateTime);
    }

    public ObjectProperty<LocalDateTime> dateTimeProperty() {
        return dateTime;
    }

    // Completed
    public boolean isCompleted() {
        return completed.get();
    }

    public void setCompleted(boolean completed) {
        this.completed.set(completed);
    }

    public BooleanProperty completedProperty() {
        return completed;
    }

    @Override
    public String toString() {
        return String.format("Reminder{id=%d, product='%s', dateTime=%s, completed=%b}",
                getId(), getProduct().getName(), getDateTime(), isCompleted());
    }
}
