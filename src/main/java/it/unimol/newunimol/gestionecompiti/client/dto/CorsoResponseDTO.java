package it.unimol.newunimol.gestionecompiti.client.dto;

import java.util.ArrayList;
import java.util.List;

public class CorsoResponseDTO {
    private String id;
    private String nome;
    private String codice;
    private List<DocenteDTO> docenti;

    public CorsoResponseDTO() {
    }

    public CorsoResponseDTO(String id, String nome, String codice, List<DocenteDTO> docenti) {
        this.id = id;
        this.nome = nome;
        this.codice = codice;
        this.docenti = docenti != null ? new ArrayList<>(docenti) : null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCodice() {
        return codice;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

    public List<DocenteDTO> getDocenti() {
        return docenti != null ? new ArrayList<>(docenti) : null;
    }

    public void setDocenti(List<DocenteDTO> docenti) {
        this.docenti = docenti != null ? new ArrayList<>(docenti) : null;
    }

    /**
     * DTO per docenti del corso.
     */
    public static class DocenteDTO {
        private String id;
        private String nome;
        private String cognome;

        public DocenteDTO() {
        }

        public DocenteDTO(String id, String nome, String cognome) {
            this.id = id;
            this.nome = nome;
            this.cognome = cognome;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getCognome() {
            return cognome;
        }

        public void setCognome(String cognome) {
            this.cognome = cognome;
        }
    }
}
