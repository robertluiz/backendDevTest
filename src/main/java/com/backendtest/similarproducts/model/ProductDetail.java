package com.backendtest.similarproducts.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing the product details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetail {
    private String id;
    private String name;
    private Double price;
    private Boolean availability;
} 