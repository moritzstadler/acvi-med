package at.ac.meduniwien.vcfvisualize.data.variantcache;

import at.ac.meduniwien.vcfvisualize.model.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

class UserSampleAccess implements Comparable<UserSampleAccess> {

    public UserSampleAccess(User user, String sample, LocalDateTime dateTime) {
        this.user = user;
        this.sample = sample;
        this.dateTime = dateTime;
    }

    @Getter
    @Setter
    User user;

    @Getter
    @Setter
    String sample;

    @Getter
    @Setter
    LocalDateTime dateTime;

    @Override
    public int compareTo(UserSampleAccess o) {
        return dateTime.compareTo(o.getDateTime());
    }

}