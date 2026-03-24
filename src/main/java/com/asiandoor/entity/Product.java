package com.asiandoor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "products")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** Slug-style category key: wooden | steel | glass | security | interior | exterior */
    private String category;

    private String material;

    private Double price;

    private String dimensions;

    private String lockSystem;

    private String imageUrl;

    @Column(length = 1000)
    private String description;
}
