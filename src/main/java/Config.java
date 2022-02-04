import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Config {
    //info_csq_consequence == missense_variant
    //info_csq_biotype == protein_coding
    //convert | to / in import
    //info_csq_sift = deleterious(0) -> 0
    //info_csq_polyphen detto

    public static final HashSet<String> specialCsqFields = new HashSet<>(Arrays.asList(
            "aloft_fraction_transcripts_affected",
            "aloft_pred",
            "aloft_prob_dominant",
            "aloft_prob_recessive",
            "fathmm_pred",
            "fathmm_score",
            "mvp_score",
            "mutationtaster_aae",
            "mutationtaster_pred",
            "mutationtaster_score",
            "polyphen2_hdiv_pred",
            "polyphen2_hdiv_score",
            "sift4g_score",
            "vest4_score",
            "codonpos"
    ));

}
