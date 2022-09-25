package at.ac.meduniwien.vcfvisualize.rest;

import at.ac.meduniwien.vcfvisualize.model.Sample;
import at.ac.meduniwien.vcfvisualize.model.Study;
import at.ac.meduniwien.vcfvisualize.model.User;
import at.ac.meduniwien.vcfvisualize.rest.dto.*;
import at.ac.meduniwien.vcfvisualize.study.StudyService;
import at.ac.meduniwien.vcfvisualize.security.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class Authentication {

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    StudyService studyService;

    @CrossOrigin
    @PostMapping("/authentication/login")
    public TokenDTO login(@RequestBody LoginDTO loginDTO) {
        TokenDTO tokenDTO = authenticationService.login(loginDTO.email, loginDTO.password);
        if (tokenDTO == null) {
            System.out.println(LocalDateTime.now() + ": " + loginDTO.email + " failed to log in");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "email or password wrong");
        } else {
            System.out.println(LocalDateTime.now() + ": " + loginDTO.email + " successfully logged in");
            return tokenDTO;
        }
    }

    @CrossOrigin
    @PostMapping("/authentication/refresh")
    public TokenDTO refresh(@RequestBody AuthenticationDTO authenticationDTO) {
        TokenDTO tokenDTO = authenticationService.refresh(authenticationDTO.tokenString);
        if (tokenDTO == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token expired");
        } else {
            return tokenDTO;
        }
    }

    @CrossOrigin
    @PostMapping("/authentication/createuser")
    public UserDTO createUser(@RequestBody CreateUserDTO createUserDTO) {
        User user = authenticationService.getUserForToken(createUserDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no admin rights");
        }

        if (authenticationService.emailExists(createUserDTO.email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email exists already");
        }

        return authenticationService.createUser(createUserDTO.email, createUserDTO.isAdmin).convertToDTO();
    }

    @CrossOrigin
    @PostMapping("/authentication/deleteuser")
    public void deleteUser(@RequestBody UserIdDTO userIdDTO) {
        User user = authenticationService.getUserForToken(userIdDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no admin rights");
        }

        authenticationService.deleteUser(userIdDTO.userId);
    }

    @CrossOrigin
    @PostMapping("/authentication/getallusers")
    public List<UserDTO> getAllUsers(@RequestBody AuthenticationDTO authenticationDTO) {
        User user = authenticationService.getUserForToken(authenticationDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no admin rights");
        }

        List<UserDTO> userDTOs = authenticationService.getAllUsers()
                .stream()
                .map(User::convertToDTO)
                .collect(Collectors.toList());

        for (UserDTO userDTO : userDTOs) {
            userDTO.studies = studyService.getStudiesForUser(new User(userDTO.id)).stream().map(Study::convertToDTO).collect(Collectors.toList());
            for (StudyDTO studyDTO : userDTO.studies) {
                studyDTO.samples = studyService.getSamplesForStudy(new Study(studyDTO.id)).stream().map(Sample::convertToDTO).collect(Collectors.toList());
            }
        }

        return userDTOs;
    }

    @CrossOrigin
    @PostMapping("/authentication/activateuser")
    public UserDTO activateUser(@RequestBody ActivateUserDTO activateUserDTO) {
        User user = authenticationService.activateUser(activateUserDTO.activationCode, activateUserDTO.password);
        if (user == null || user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "activation code invalid");
        }
        return user.convertToDTO();
    }

}
