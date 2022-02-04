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
            "info_csq_aloft_fraction_transcripts_affected",
            "info_csq_aloft_pred",
            "info_csq_aloft_prob_dominant",
            "info_csq_aloft_prob_recessive",
            "info_csq_fathmm_pred",
            "info_csq_fathmm_score",
            "info_csq_mvp_score",
            "info_csq_mutationtaster_aae",
            "info_csq_mutationtaster_pred",
            "info_csq_mutationtaster_score",
            "info_csq_polyphen2_hdiv_pred",
            "info_csq_polyphen2_hdiv_score",
            "info_csq_sift4g_score",
            "info_csq_vest4_score",
            "info_csq_codonpos"
    ));

}
