package it.unimol.newunimol.gestionecompiti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public final class NewUnimolApplication {

    private NewUnimolApplication() {
        // Private constructor to hide implicit public one
    }

    public static void main(String[] args) {
        SpringApplication.run(NewUnimolApplication.class, args);
    }

}
