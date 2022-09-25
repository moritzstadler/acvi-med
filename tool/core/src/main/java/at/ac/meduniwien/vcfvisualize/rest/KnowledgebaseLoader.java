package at.ac.meduniwien.vcfvisualize.rest;

import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.Gene;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.Panel;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.PanelApp;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.PanelDTO;
import at.ac.meduniwien.vcfvisualize.model.User;
import at.ac.meduniwien.vcfvisualize.rest.dto.AuthenticationDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.FieldMetaDataDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.genepanelsearch.GenePanelIndexDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.genepanelsearch.IndexedGeneAliasDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.genepanelsearch.IndexedGeneDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.genepanelsearch.IndexedPanelDTO;
import at.ac.meduniwien.vcfvisualize.security.AuthenticationService;
import at.ac.meduniwien.vcfvisualize.security.ConfigurationService;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class KnowledgebaseLoader {

    @Autowired
    PanelApp panelApp;

    @Autowired
    AuthenticationService authenticationService;

    @CrossOrigin
    @PostMapping("/knowledgebase/panelapp/getindex")
    public GenePanelIndexDTO getAll(@RequestBody AuthenticationDTO authenticationDTO) {
        //evaluate whether this is necessary given it it public information
        User user = authenticationService.getUserForToken(authenticationDTO.tokenString);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }

        return getGenePanelIndexDTO();
    }

    private GenePanelIndexDTO getGenePanelIndexDTO() {
        GenePanelIndexDTO genePanelIndexDTO = new GenePanelIndexDTO();
        genePanelIndexDTO.panelIndex = new ArrayList<>();
        genePanelIndexDTO.geneIndex = new ArrayList<>();
        genePanelIndexDTO.aliasIndex = new ArrayList<>();

        List<Panel> panels = panelApp.getPanels();

        HashMap<String, Gene> genes = new HashMap<>();
        for (Panel panel : panels) {
            genePanelIndexDTO.panelIndex.add(new IndexedPanelDTO(panel));
            for (Gene gene : panel.getGenes()) {
                //TODO merge here instead of overwrite! this is important!
                genes.put(gene.getGene().entity_name, gene);
            }
        }

        for (String symbol : genes.keySet()) {
            Gene gene = genes.get(symbol);
            String[] panelNames = panelApp.getGene(symbol).stream().map(g -> panelApp.getPanel(g.getId()).getPanel().name).toArray(String[]::new);
            genePanelIndexDTO.geneIndex.add(new IndexedGeneDTO(gene, panelNames));
        }

        HashMap<String, String> aliases = panelApp.getAliases();
        for (String alias : aliases.keySet()) {
            genePanelIndexDTO.aliasIndex.add(new IndexedGeneAliasDTO(alias, aliases.get(alias)));
        }

        //add other genes
        if (allIndexedGeneAliasDTOs == null || allIndexedGeneDTOs == null) {
            loadAllGenes(genePanelIndexDTO.geneIndex.stream().map(g -> g.symbol).collect(Collectors.toSet()));
        }
        genePanelIndexDTO.geneIndex.addAll(allIndexedGeneDTOs);
        genePanelIndexDTO.aliasIndex.addAll(allIndexedGeneAliasDTOs);

        return genePanelIndexDTO;
    }

    ArrayList<IndexedGeneDTO> allIndexedGeneDTOs;
    ArrayList<IndexedGeneAliasDTO> allIndexedGeneAliasDTOs;

    /**
     * loads all genes excluding the ones coming from panel app
     * @param excludedSymbols these are not loaded
     */
    @SneakyThrows
    private void loadAllGenes(Set<String> excludedSymbols) {
        System.out.println("parsing genes");

        allIndexedGeneDTOs = new ArrayList<>();
        allIndexedGeneAliasDTOs = new ArrayList<>();

        String genesJsonString = ConfigurationService.read("genes.json");
        JSONArray genes = new JSONArray(genesJsonString);
        for (int i = 0; i < genes.length(); i++) {
            JSONObject gene = genes.getJSONObject(i);

            String symbol = gene.getString("symbol");
            if (!excludedSymbols.contains(symbol)) {
                IndexedGeneDTO indexedGeneDTO = new IndexedGeneDTO(symbol);
                indexedGeneDTO.name = gene.getString("name");
                indexedGeneDTO.omim = gene.getString("omim").split(",");

                allIndexedGeneDTOs.add(indexedGeneDTO);

                allIndexedGeneAliasDTOs.addAll(Arrays.stream(gene.getString("aliases").split(",")).map(a -> new IndexedGeneAliasDTO(a, symbol)).collect(Collectors.toList()));
            }
        }
    }

}
