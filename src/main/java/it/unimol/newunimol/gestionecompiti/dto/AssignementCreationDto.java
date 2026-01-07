package it.unimol.newunimol.gestionecompiti.dto;

public record AssignementCreationDto(
                String id,
                String titolo,
                String descrizione,
                Long dataCreazione,
                Long dataScadenza,
                AllegatoDto[] allegati) {

}
