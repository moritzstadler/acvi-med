package at.ac.meduniwien.vcfvisualize.mocking;

import at.ac.meduniwien.vcfvisualize.model.Filter;
import at.ac.meduniwien.vcfvisualize.model.User;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@Service
public class MockVariantProvider {
    public boolean isValidSampleId(String sample) {
        return true;
    }

    public List<Variant> getVariants(User user, String sample, Filter filter) {
        List<Variant> variants = new LinkedList<>();

        Variant variant1 = new Variant();
        variant1.setPid(1025);
        variant1.setVid(2049);
        variant1.setChrom("chr4");
        variant1.setPos("1048577");
        variant1.setId("1");
        variant1.setRef("TTGG");
        variant1.setAlt("C");
        variant1.setQual("Pass");
        variant1.setFilter(".");
        variant1.setFormat("format");
        HashMap<String, String> info1 = new HashMap<>();
        info1.put("info_af_raw", "0.00023");
        info1.put("info_csq_consequence", "inframe_deletion");
        info1.put("info_csq_hgvsc", "ENST00000657896.1:n.563TTGG>C");
        variant1.setInfo(info1);
        variants.add(variant1);

        Variant variant2 = new Variant();
        variant2.setPid(1024);
        variant2.setVid(2048);
        variant2.setChrom("chr1");
        variant2.setPos("1048576");
        variant2.setId("1");
        variant2.setRef("A");
        variant2.setAlt("CAAA");
        variant2.setQual("Pass");
        variant2.setFilter(".");
        variant2.setFormat("format");
        HashMap<String, String> info2 = new HashMap<>();
        info2.put("info_af_raw", "0.5");
        info2.put("info_csq_consequence", "inframe_insertion");
        info2.put("info_csq_hgvsc", "ENST00000443772.2:n.121+3914A>CAAA");
        variant2.setInfo(info2);
        variants.add(variant2);

        return variants;
    }
}
