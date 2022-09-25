package at.ac.meduniwien.vcfvisualize.data.variantcache;

import at.ac.meduniwien.vcfvisualize.model.User;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

class UserSamplePair {

    public UserSamplePair(User user, String sample) {
        this.user = user;
        this.sample = sample;
    }

    @Getter
    @Setter
    User user;

    @Getter
    @Setter
    String sample;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSamplePair that = (UserSamplePair) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(sample, that.sample);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, sample);
    }
}
