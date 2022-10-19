package at.ac.meduniwien.vcfvisualize.knowledgebase.hpo;

import at.ac.meduniwien.vcfvisualize.knowledgebase.ApiRequest;
import at.ac.meduniwien.vcfvisualize.knowledgebase.ApiResponseBody;
import at.ac.meduniwien.vcfvisualize.knowledgebase.RestClient;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.Gene;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.PanelIdGene;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.GeneDTO;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.PanelDTO;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.PanelResponseDTO;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.PanelsResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class Hpo {

    @Autowired
    RestClient restClient;

    HashMap<String, HpoTerm> hpoTermById;

    public Hpo() {
        hpoTermById = new HashMap<>();
    }

    public void loadDataFromAPI() {
        System.out.println("loading HPO");
        String apiResponse = geHpoTermsFromAPISkippingCache();
        parseHpoTerms(apiResponse);
        System.out.println(hpoTermById.size() + " HPO terms found");
    }

    private void parseHpoTerms(String apiResponse) {
        HashMap<String, HpoTerm> newHpoTerms = new HashMap<>();
        String[] stanzas = apiResponse.split("\n\n");

        for (String stanza : stanzas) {
            String[] lines = stanza.split("\n");
            if (lines.length > 0 && lines[0].equals("[Term]")) {
                HpoTerm hpoTerm = new HpoTerm();
                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i];
                    String[] keyValuePair = line.split(":");
                    String key = keyValuePair[0];
                    String value = "";

                    for (int j = 1; j < keyValuePair.length; j++) {
                        value += keyValuePair[j];
                        if (j != keyValuePair.length - 1) {
                            value += ":";
                        }
                    }

                    value = value.trim();

                    if (key.equals("id")) {
                        hpoTerm.setId(value);
                    } else if (key.equals("alt_id")) {
                        hpoTerm.getAltIds().add(value);
                    } else if (key.equals("name")) {
                        hpoTerm.setName(value);
                    } else if (key.equals("def")) {
                        hpoTerm.setDef(value);
                    } else if (key.equals("synonym")) {
                        hpoTerm.getSynonym().add(value);
                    }
                }
                newHpoTerms.put(hpoTerm.getId(), hpoTerm);
            }
        }

        this.hpoTermById = newHpoTerms;
    }

    private String geHpoTermsFromAPISkippingCache() {
        String url = "https://raw.githubusercontent.com/obophenotype/human-phenotype-ontology/master/hp.obo";
        ApiRequest apiRequest = new ApiRequest(url, null);
        return restClient.callApiRaw(apiRequest);
    }

}
