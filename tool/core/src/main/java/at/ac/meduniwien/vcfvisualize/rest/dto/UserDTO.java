package at.ac.meduniwien.vcfvisualize.rest.dto;

import java.util.List;

public class UserDTO {

    public long id;
    public String email;
    public boolean isAdmin;
    public boolean isActive;
    public String activationCode;
    public List<StudyDTO> studies;

}
