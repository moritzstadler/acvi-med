package at.ac.meduniwien.vcfvisualize.rest.dto;

import java.util.List;

public class PhenotypeAwareLoadRequestDTO {

    public String token;
    public String sample;

    public List<String> hpoTerms;
    public List<String> genes;

    public HumansDTO humansDTO;
}

