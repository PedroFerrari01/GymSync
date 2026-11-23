package com.example.sistema.academia.view;

import com.example.sistema.academia.service.MarcaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Bean @marca para os templates: identidade da academia (cor, nome e logo) sem lógica no HTML. */
@Component("marca")
public class MarcaView {

    @Autowired private MarcaService service;

    public boolean temCor() { return service.marca().temCor(); }
    public boolean temLogo() { return service.marca().temLogo(); }
    public boolean logoClara() { return service.marca().logoClara(); }
    public String versao() { return service.marca().versao(); }
    public String cor() { String c = service.marca().cor(); return c == null ? "#f97316" : c; }

    private static final java.util.Set<String> PREDEFINIDAS =
            java.util.Set.of("#f97316", "#2563eb", "#16a34a", "#7c3aed", "#dc2626", "#52525b");

    /** Cor própria em uso (a que não é uma das predefinidas), para preencher o campo; vazio se não houver. */
    public String corPropria() {
        String c = service.marca().cor();
        return c == null || PREDEFINIDAS.contains(c) ? "" : c;
    }

    /** Nome definido pela academia, ou null. */
    public String nomePersonalizado() { return service.marca().nome(); }
    public boolean temNome() { return service.marca().nome() != null; }
}
