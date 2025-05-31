package com.skincaretracker.model;

import javafx.beans.property.*;

public class Product {
    private final StringProperty name;
    private final StringProperty description;
    private final BooleanProperty allergic;
    private final IntegerProperty rating;
    private final LongProperty id;

    public Product(Long id, String name, String description, boolean allergic, int rating) {
        this.id = new SimpleLongProperty(id);
        this.name = new SimpleStringProperty(name);
        this.description = new SimpleStringProperty(description);
        this.allergic = new SimpleBooleanProperty(allergic);
        this.rating = new SimpleIntegerProperty(rating);
    }

    // ID
    public Long getId() {
        return id.get();
    }

    public LongProperty idProperty() {
        return id;
    }

    // Name
    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    // Description
    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    // Allergic
    public boolean isAllergic() {
        return allergic.get();
    }

    public void setAllergic(boolean allergic) {
        this.allergic.set(allergic);
    }

    public BooleanProperty allergicProperty() {
        return allergic;
    }

    // Rating
    public int getRating() {
        return rating.get();
    }

    public void setRating(int rating) {
        if (rating >= 0 && rating <= 5) {
            this.rating.set(rating);
        }
    }

    public IntegerProperty ratingProperty() {
        return rating;
    }

    @Override
    public String toString() {
        return String.format("Product{id=%d, name='%s', allergic=%b, rating=%d}",
                getId(), getName(), isAllergic(), getRating());
    }
}
