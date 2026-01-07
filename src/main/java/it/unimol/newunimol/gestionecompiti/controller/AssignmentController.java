package it.unimol.newunimol.gestionecompiti.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/assignment")
@Tag(name = "Assignment", description = "API per la gestione dei compiti")
public class AssignmentController {
    @Autowired
    private AssignmentService assignmentService;

    public void createNewAssignment() {
        // prende in input un dto che
        // contiene : Titolo, descrizione, data di scadenza, allegati
    }
}
