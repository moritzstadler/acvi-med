package at.ac.meduniwien.vcfvisualize.knowledgebase.acmg;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SecondaryFindings {

    @SneakyThrows
    public List<SecondaryFindingDefinition> getDefinitions() {
        List<SecondaryFindingDefinition> result = new LinkedList<>();

        Path varPath = new File("src/main/resources/acmg/secondary_findings.csv").toPath();
        if (Files.exists(varPath)) {
            String input = Files.readString(varPath);
            String[] lines = input.split("\n");
            for (int i = 1; i < lines.length; i++) { //start at 1 to skip the header
                String[] s = lines[i].split(";");
                SecondaryFindingDefinition sfd = new SecondaryFindingDefinition();

                //Gene;Inheritance;Onset child;Onset adult;Report KP;Report EP;Domain;Phenotype;OMIM Id;ACMG SF list version
                sfd.setGene(s[0]);
                sfd.setInheritance(s[1]);
                sfd.setTypicalOnsetChild(s[2].equals("1"));
                sfd.setTypicalOnsetAdult(s[3].equals("1"));
                sfd.setReportKnownPathogenic(s[4].equals("1"));
                sfd.setReportExpectedPathogenic(s[5].equals("1"));
                sfd.setDomain(s[6]);
                sfd.setPhenotype(s[7]);
                sfd.setOmimId(Arrays.stream(s[8].split(",")).collect(Collectors.toList()));
                sfd.setAcmgListVersion(s[9]);

                result.add(sfd);
            }
        }

        return result;
    }

}
