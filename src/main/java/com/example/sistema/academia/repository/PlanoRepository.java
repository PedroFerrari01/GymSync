package com.example.sistema.academia.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.sistema.academia.model.Plano;

public interface PlanoRepository extends JpaRepository<Plano, Long> {
}