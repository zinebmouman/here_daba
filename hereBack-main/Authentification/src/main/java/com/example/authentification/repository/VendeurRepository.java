package com.example.authentification.repository;
import com.example.authentification.model.Vendeur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface VendeurRepository extends JpaRepository<Vendeur, Long> {
    Optional<Vendeur> findByIdVendeur(String idVendeur);

    void deleteByIdVendeur(String idVendeur);
}