package at.ac.meduniwien.vcfvisualize.processor.acmg;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

public class AcmgTieringResult {

    @Getter
    @Setter
    private AcmgTier tier;

    @Getter
    private boolean tierApplies;

    public AcmgTieringResult setTierApplies(boolean tierApplies) {
        this.tierApplies = tierApplies;
        return this;
    }

    @Getter
    @Setter
    private HashMap<String, String> explanation;

    public AcmgTieringResult(boolean tierApplies) {
        this.tierApplies = tierApplies;
        this.explanation = new HashMap<>();
    }

    public AcmgTieringResult() {
        this.explanation = new HashMap<>();
    }

    public void addExplanation(String title, String value) {
        this.explanation.put(title, value);
    }

    public void addExplanation(String title, int value) {
        this.explanation.put(title, value + "");
    }

    public void addExplanation(String title, double value) {
        this.explanation.put(title, value + "");
    }
}
