package com.backendtest.similarproducts.model;

import java.util.Objects;

/**
 * Model representing the product details
 */
public class ProductDetail {
    private String id;
    private String name;
    private Double price;
    private Boolean availability;

    public ProductDetail() {
    }

    public ProductDetail(String id, String name, Double price, Boolean availability) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.availability = availability;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Boolean getAvailability() {
        return availability;
    }

    public void setAvailability(Boolean availability) {
        this.availability = availability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductDetail that = (ProductDetail) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(price, that.price) &&
                Objects.equals(availability, that.availability);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, availability);
    }

    @Override
    public String toString() {
        return "ProductDetail{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", availability=" + availability +
                '}';
    }
} 