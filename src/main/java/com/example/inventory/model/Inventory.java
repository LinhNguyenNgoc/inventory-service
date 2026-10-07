package com.example.inventory.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String skuCode;
    private Integer stockQuantity;
    private String warehouseLocation;
}