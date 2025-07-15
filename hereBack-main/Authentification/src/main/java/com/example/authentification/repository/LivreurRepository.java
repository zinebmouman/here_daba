package com.example.authentification.repository;

import com.example.authentification.model.Livreur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LivreurRepository extends JpaRepository<Livreur, Long> {
    Optional<Livreur> findByIdLivreur(String idLivreur);

    void deleteByIdLivreur(String idLivreur);
}