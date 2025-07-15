package com.servicepaiement.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "nom_produit", nullable = false)
    private String nomProduit;

    @Column(name = "prix", nullable = false)
    private Double prix;

    @Column(name = "quantite", nullable = false)
    private Integer quantite;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "categorie")
    private String categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
}