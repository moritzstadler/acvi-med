package at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp;

import at.ac.meduniwien.vcfvisualize.knowledgebase.ApiRequest;
import at.ac.meduniwien.vcfvisualize.knowledgebase.RestClient;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.GeneDTO;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.PanelDTO;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.PanelResponseDTO;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.PanelsResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@Service
public class PanelApp {

    @Autowired
    RestClient restClient;

    private List<Panel> panels;
    private HashMap<Integer, Panel> panelById;
    private HashMap<String, List<PanelIdGene>> geneByName; //the same gene can have different values in different panels
    private HashMap<String, String> aliasToGeneName;

    public PanelApp() {
        this.panelById = new HashMap<>();
        this.geneByName = new HashMap<>();
        this.aliasToGeneName = new HashMap<>();
    }

    public List<Panel> getPanels() {
        if (panels == null || panels.isEmpty()) {
            //this takes some minutes so it is just a fallback to avoid blocking - the method is called by a cronjob or via the admin panel
            loadDataFromAPI();
        }

        return panels;
    }

    /**
     * retrieves a panel by its id
     * can be used in parsing filters
     * @param id the id of the panel
     * @return the found panel or null
     */
    public Panel getPanel(int id) {
        return panelById.get(id);
    }

    public HashMap<String, String> getAliases() {
        return aliasToGeneName;
    }

    /**
     * retrieves data about a gene
     * can be used in showing data about a single or multiple variants
     * @param name the name or an alias of the gene
     * @return the gene
     */
    public List<PanelIdGene> getGene(String name) {
        String standardizedName = name;
        if (aliasToGeneName.containsKey(name)) {
            standardizedName = aliasToGeneName.get(name);
        }
        return geneByName.get(name);
    }

    public void loadDataFromAPI() {
        ArrayList<PanelDTO> panelDTOs = getAllPanelsFromAPISkippingCache();
        panels = new LinkedList<>();
        panelById = new HashMap<>();
        geneByName = new HashMap<>();

        int dbgCnt = 0;
        for (PanelDTO panelDTO : panelDTOs) {
            dbgCnt++;
            System.out.println("loading panel (" + dbgCnt + "/" + panelDTOs.size() + ") " + panelDTO.id + " " + panelDTO.name);
            try {
                PanelResponseDTO panelResponseDTO = getPanelByIdFromAPISkippingCache(panelDTO.id);
                Panel panel = new Panel(panelResponseDTO);

                panels.add(panel);
                panelById.put(panelDTO.id, panel);

                List<GeneDTO> geneDTOs = List.of(panelResponseDTO.genes);
                for (GeneDTO geneDTO : geneDTOs) {
                    Gene gene = new Gene(geneDTO);

                    String geneName = gene.getGene().entity_name;
                    if (!geneByName.containsKey(geneName)) {
                        geneByName.put(geneName, new LinkedList<>());
                    }
                    geneByName.get(geneName).add(new PanelIdGene(panelDTO.id, gene));

                    for (String alias : geneDTO.gene_data.alias) {
                        aliasToGeneName.put(alias, geneDTO.entity_name);
                    }

                    panel.getGenes().add(gene);
                }

            } catch (Exception e) {
                System.out.println("error in panel '" + panelDTO.name + "' id: " + panelDTO.id);
                System.out.println(e.getMessage());
            }
        }

        System.out.println("panels: " + panels.size());
        System.out.println("overall genes: " + panels.stream().map(p -> p.genes.size()).reduce(0, Integer::sum));
        System.out.println("individual genes: " + geneByName.size());
        System.out.println("individual aliases: " + aliasToGeneName.size());

        //TODO make panelsString
    }

    private ArrayList<PanelDTO> getAllPanelsFromAPISkippingCache() {
        ArrayList<PanelDTO> result = new ArrayList<>();

        //due to pagination we have to retrieve a full list of panels this way
        PanelsResponseDTO currentResponse;
        int pageNumber = 1;
        do {
            String url = String.format("https://panelapp.genomicsengland.co.uk/api/v1/panels/?page=%s", pageNumber);
            ApiRequest apiRequest = new ApiRequest(url, null);
            currentResponse = (PanelsResponseDTO) restClient.performRequestSkipCache(apiRequest, new PanelsResponseDTO()).getApiResponseBody();
            result.addAll(List.of(currentResponse.results));
            pageNumber++;
        } while (currentResponse.next != null);

        return result;
    }

    private PanelResponseDTO getPanelByIdFromAPISkippingCache(int id) {
        String url = String.format("https://panelapp.genomicsengland.co.uk/api/v1/panels/%s/", id);
        ApiRequest apiRequest = new ApiRequest(url, null);
        return (PanelResponseDTO) restClient.performRequestSkipCache(apiRequest, new PanelResponseDTO()).getApiResponseBody();
    }

}
