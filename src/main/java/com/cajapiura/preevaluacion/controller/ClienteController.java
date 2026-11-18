package com.cajapiura.preevaluacion.controller;

import com.cajapiura.preevaluacion.model.Cliente;
import com.cajapiura.preevaluacion.service.ClienteService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Arrays;

/** Portal de clientes: registro, inicio de sesión y panel personal. */
@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    private static boolean esCliente(Authentication auth) {
        return auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority()));
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String bloqueado,
                        @RequestParam(required = false) String registrado,
                        @RequestParam(required = false) String acceso,
                        Authentication auth, Model model) {
        if (esCliente(auth)) {
            return "redirect:/cliente";
        }
        model.addAttribute("error", error != null);
        model.addAttribute("bloqueado", bloqueado != null);
        model.addAttribute("registrado", registrado != null);
        model.addAttribute("acceso", acceso != null);
        return "cliente/login";
    }

    @GetMapping("/registro")
    public String registro(Authentication auth) {
        return esCliente(auth) ? "redirect:/cliente" : "cliente/registro";
    }

    @PostMapping("/registro")
    public String registrar(@RequestParam String nombreCompleto,
                            @RequestParam String correo,
                            @RequestParam String password,
                            @RequestParam String confirmar,
                            @RequestParam(defaultValue = "false") boolean aceptaDatos,
                            @RequestParam(defaultValue = "false") boolean aceptaTerminos,
                            Model model) {
        try {
            service.registrar(nombreCompleto, correo, password, confirmar, aceptaDatos, aceptaTerminos);
            return "redirect:/cliente/login?registrado";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errores", Arrays.asList(e.getMessage().split("\n")));
            model.addAttribute("nombreCompleto", nombreCompleto);
            model.addAttribute("correo", correo);
            return "cliente/registro";
        }
    }

    @GetMapping
    public String panel(Principal principal, Model model) {
        Cliente cliente = service.buscarPorCorreo(principal.getName()).orElseThrow();
        model.addAttribute("cliente", cliente);
        model.addAttribute("solicitudes", service.solicitudesDe(cliente));
        return "cliente/panel";
    }
}
