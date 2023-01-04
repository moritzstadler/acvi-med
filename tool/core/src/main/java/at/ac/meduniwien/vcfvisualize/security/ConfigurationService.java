package at.ac.meduniwien.vcfvisualize.security;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ConfigurationService {

    /**
     * checks if a file called filename is stored in /var and returns its contents
     * if no matching file could by found in /var it is taken from src/main/resources instead
     *
     * @param filename the filename (not path)
     * @return string with the file's contents
     */
    @SneakyThrows
    public static String read(String filename) {
        Path varPath = new File("var/" + filename).toPath();
        if (Files.exists(varPath)) {
            return Files.readString(varPath);
        }

        Path resourcesPath = new File("src/main/resources/defaultconfiguration/" + filename).toPath();
        if (Files.exists(resourcesPath)) {
            return Files.readString(resourcesPath);
        }

        System.out.println("Could not locate file " + filename + " in var/ or src/main/resources/defaultconfiguration/! Make sure to add it.");
        return "";
    }

}
