package at.ac.meduniwien.vcfvisualize.model;

import at.ac.meduniwien.vcfvisualize.rest.dto.UserDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class User {

    @Getter
    @Setter
    long id;

    @Getter
    @Setter
    String email;

    @Getter
    @Setter
    String hashedPassword;

    @Getter
    @Setter
    String salt;

    @Getter
    @Setter
    boolean isActive;

    @Getter
    @Setter
    String activationCode;

    @Getter
    @Setter
    boolean isAdmin;

    public User(long id) {
        this.id = id;
    }

    public User(String email) {
        this.email = email;
    }

    public User(long id, String email, String hashedPassword, String salt, boolean isActive, String activationCode, boolean isAdmin) {
        this.id = id;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.salt = salt;
        this.isActive = isActive;
        this.activationCode = activationCode;
        this.isAdmin = isAdmin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    public UserDTO convertToDTO() {
        UserDTO userDTO = new UserDTO();
        userDTO.id = this.id;
        userDTO.activationCode = this.activationCode;
        userDTO.email = this.email;
        userDTO.isActive = this.isActive;
        userDTO.isAdmin = this.isAdmin;
        return userDTO;
    }
}
