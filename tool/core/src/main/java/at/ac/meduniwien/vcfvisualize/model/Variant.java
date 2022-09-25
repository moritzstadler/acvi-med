package at.ac.meduniwien.vcfvisualize.model;

import at.ac.meduniwien.vcfvisualize.rest.dto.VariantDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Set;

public class Variant {

    public Variant() {
        info = new HashMap<>();
    }

    @Getter
    @Setter
    long pid;

    @Getter
    @Setter
    long vid;

    @Getter
    @Setter
    String chrom;

    @Getter
    @Setter
    String pos;

    @Getter
    @Setter
    String id;

    @Getter
    @Setter
    String ref;

    @Getter
    @Setter
    String alt;

    @Getter
    @Setter
    String qual;

    @Getter
    @Setter
    String filter;

    @Getter
    @Setter
    HashMap<String, String> info;

    @Getter
    @Setter
    String format;

    /**
     * returns variant as dto
     * @return the converted dto
     */
    public VariantDTO convertToDTO() {
        VariantDTO dto = new VariantDTO();
        dto.setPid(pid);
        dto.setVid(vid);
        dto.setChrom(chrom);
        dto.setPos(pos);
        dto.setId(id);
        dto.setRef(ref);
        dto.setAlt(alt);
        dto.setQual(qual);
        dto.setFilter(filter);
        dto.setInfo(info);
        dto.setFormat(format);
        return dto;
    }

    /**
     * creates a smaller dto for the overview page
     * In test scenarios this reduced the size roughly 15 fold!
     * @return a smaller dto
     */
    public VariantDTO convertToReducedDTO(Set<String> infoFieldsToKeep) {

        HashMap<String, String> reducedInfo = new HashMap<>();

        for (String key : info.keySet()) {
            if (infoFieldsToKeep.contains(key) || (key.startsWith("format_") && key.endsWith("_gt"))) {
                reducedInfo.put(key, info.get(key));
            }
        }

        VariantDTO variantDTO = convertToDTO();
        variantDTO.setInfo(reducedInfo);
        return variantDTO;
    }

    public VariantIdentifier getVariantIdentifier() {
        return new VariantIdentifier(pid);
    }

}
