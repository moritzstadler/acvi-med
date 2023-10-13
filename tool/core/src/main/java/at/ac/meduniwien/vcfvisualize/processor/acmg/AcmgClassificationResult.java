package at.ac.meduniwien.vcfvisualize.processor.acmg;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

public class AcmgClassificationResult {

    @Getter
    @Setter
    private AcmgClassification acmgClassification;

    @Getter
    @Setter
    private HashMap<String, String> explanation;

    public AcmgClassificationResult() {
        this.explanation = new HashMap<>();
    }

    public void addExplanation(String title, String value) {
        this.explanation.put(title, value);
    }
}
