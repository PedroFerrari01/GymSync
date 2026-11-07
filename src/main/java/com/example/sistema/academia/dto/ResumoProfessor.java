package com.example.sistema.academia.dto;

/** Uma linha da visão geral da equipe (admin): carga e adesão de cada professor. */
public record ResumoProfessor(String nome, String especialidade, int fichas, int alunos, int sumidos, int adesaoMedia) {}
