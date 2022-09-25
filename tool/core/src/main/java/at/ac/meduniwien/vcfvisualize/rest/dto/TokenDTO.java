package at.ac.meduniwien.vcfvisualize.rest.dto;

import java.util.Date;

public class TokenDTO {
    public UserDTO user;
    public Date expiryTime;
    public String tokenString;
}
