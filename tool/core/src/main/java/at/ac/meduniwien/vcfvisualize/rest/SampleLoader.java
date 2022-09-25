package at.ac.meduniwien.vcfvisualize.rest;

import at.ac.meduniwien.vcfvisualize.model.Sample;
import at.ac.meduniwien.vcfvisualize.model.Study;
import at.ac.meduniwien.vcfvisualize.model.User;
import at.ac.meduniwien.vcfvisualize.rest.dto.*;
import at.ac.meduniwien.vcfvisualize.security.AuthenticationService;
import at.ac.meduniwien.vcfvisualize.study.StudyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SampleLoader {

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    StudyService studyService;

    @CrossOrigin
    @PostMapping("/sample/synchronizesamples")
    public void synchronizeSamples(@RequestBody AuthenticationDTO authenticationDTO) {
        User user = authenticationService.getUserForToken(authenticationDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no admin rights");
        }

        studyService.synchronizeSamples();
    }

    @CrossOrigin
    @PostMapping("/study/getall")
    public List<StudyDTO> getAllStudies(@RequestBody AuthenticationDTO authenticationDTO) {
        User user = authenticationService.getUserForToken(authenticationDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no admin rights");
        }

        List<Study> studies = studyService.getAllStudies();
        for (Study study : studies) {
            study.setSamples(studyService.getSamplesForStudy(study));
        }

        return studies
                .stream()
                .map(Study::convertToDTO)
                .collect(Collectors.toList());
    }

    @CrossOrigin
    @PostMapping("/sample/getall")
    public List<SampleDTO> getAllSamples(@RequestBody AuthenticationDTO authenticationDTO) {
        User user = authenticationService.getUserForToken(authenticationDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no admin rights");
        }

        return studyService.getAllSamples()
                .stream()
                .map(Sample::convertToDTO)
                .collect(Collectors.toList());
    }

    @CrossOrigin
    @PostMapping("/study/addtouser")
    public void addToUser(@RequestBody StudyUserDTO studyUserDTO) {
        User user = authenticationService.getUserForToken(studyUserDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no admin rights");
        }

        studyService.addStudyToUser(new Study(studyUserDTO.studyId), new User(studyUserDTO.userId));
    }

    @CrossOrigin
    @PostMapping("/study/removefromuser")
    public void removeFromUser(@RequestBody StudyUserDTO studyUserDTO) {
        User user = authenticationService.getUserForToken(studyUserDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no admin rights");
        }

        studyService.removeStudyFromUser(new Study(studyUserDTO.studyId), new User(studyUserDTO.userId));
    }

    @CrossOrigin
    @PostMapping("/sample/addtostudy")
    public void addToStudy(@RequestBody SampleStudyDTO sampleStudyDTO) {
        User user = authenticationService.getUserForToken(sampleStudyDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no admin rights");
        }

        studyService.addSampleToStudy(new Sample(sampleStudyDTO.sampleId), new Study(sampleStudyDTO.studyId));
    }

    @CrossOrigin
    @PostMapping("/sample/removefromstudy")
    public void removeFromStudy(@RequestBody SampleStudyDTO sampleStudyDTO) {
        User user = authenticationService.getUserForToken(sampleStudyDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no admin rights");
        }

        studyService.removeSampleFromStudy(new Sample(sampleStudyDTO.sampleId), new Study(sampleStudyDTO.studyId));
    }

    @CrossOrigin
    @PostMapping("/study/getforuser")
    public List<StudyDTO> getForUser(@RequestBody AuthenticationDTO authenticationDTO) {
        User user = authenticationService.getUserForToken(authenticationDTO.tokenString);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }

        List<Study> studies = studyService.getStudiesForUser(user);
        for (Study study : studies) {
            study.setSamples(studyService.getSamplesForStudy(study));
        }

        return studies
                .stream()
                .map(Study::convertToDTO)
                .collect(Collectors.toList());
    }

    @CrossOrigin
    @PostMapping("/study/getforuserbyadmin")
    public List<StudyDTO> getForUserByAdmin(@RequestBody UserIdDTO userIdDTO) {
        User user = authenticationService.getUserForToken(userIdDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }

        return studyService.getStudiesForUser(new User(userIdDTO.userId))
                .stream()
                .map(Study::convertToDTO)
                .collect(Collectors.toList());
    }


    @CrossOrigin
    @PostMapping("/sample/getforstudy")
    public List<SampleDTO> getForStudy(@RequestBody StudyIdDTO studyIdDTO) {
        User user = authenticationService.getUserForToken(studyIdDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }

        return studyService.getSamplesForStudy(new Study(studyIdDTO.studyId))
                .stream()
                .map(Sample::convertToDTO)
                .collect(Collectors.toList());
    }

    @CrossOrigin
    @PostMapping("/sample/update")
    public void getForStudy(@RequestBody SampleUserDTO sampleUserDTO) {
        User user = authenticationService.getUserForToken(sampleUserDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }

        studyService.updateSample(new Sample(sampleUserDTO.name, sampleUserDTO.type, sampleUserDTO.igvPath));
    }

    @CrossOrigin
    @PostMapping("/study/create")
    public void createStudy(@RequestBody CreateStudyDTO createStudyDTO) {
        User user = authenticationService.getUserForToken(createStudyDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no admin rights");
        }

        studyService.createStudy(new Study(createStudyDTO.name, createStudyDTO.type));
    }

    @CrossOrigin
    @PostMapping("/study/delete")
    public void deleteStudy(@RequestBody StudyIdDTO studyIdDTO) {
        User user = authenticationService.getUserForToken(studyIdDTO.tokenString);

        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no admin rights");
        }

        studyService.deleteStudy(new Study(studyIdDTO.studyId));
    }

}
