package at.ac.meduniwien.vcfvisualize.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * object to store an identification for a variant in a sample
 */
public class VariantIdentifier {

    public VariantIdentifier(long pid) {
        this.pid = pid;
    }

    @Getter
    @Setter
    long pid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariantIdentifier that = (VariantIdentifier) o;
        return Objects.equals(pid, that.pid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid);
    }
}
