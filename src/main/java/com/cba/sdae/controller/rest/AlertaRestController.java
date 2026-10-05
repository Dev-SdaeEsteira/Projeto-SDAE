package com.cba.sdae.controller.rest;

import com.cba.sdae.model.Alerta;
import com.cba.sdae.service.AlertaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
public class AlertaRestController {

    private final AlertaService alertaService;

    public AlertaRestController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    // GET /api/alertas — retorna todos em JSON (mais recente primeiro)
    @GetMapping
    public List<Alerta> listar() {
        return alertaService.listarTodos();
    }

    // GET /api/alertas/1 — retorna um alerta ou 404
    @GetMapping("/{id}")
    public ResponseEntity<Alerta> buscarPorId(@PathVariable Long id) {
        return alertaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/alertas/1 — deleta um alerta
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (alertaService.buscarPorId(id).isEmpty()) return ResponseEntity.notFound().build();
        alertaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}