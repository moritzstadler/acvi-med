package at.ac.meduniwien.vcfvisualize.rest;

import at.ac.meduniwien.vcfvisualize.data.VariantProvider;
import at.ac.meduniwien.vcfvisualize.model.Note;
import at.ac.meduniwien.vcfvisualize.model.Sample;
import at.ac.meduniwien.vcfvisualize.model.Study;
import at.ac.meduniwien.vcfvisualize.model.User;
import at.ac.meduniwien.vcfvisualize.rest.dto.AuthenticationDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.CreateNoteDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.NoteDTO;
import at.ac.meduniwien.vcfvisualize.security.AuthenticationService;
import at.ac.meduniwien.vcfvisualize.study.NoteService;
import at.ac.meduniwien.vcfvisualize.study.StudyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class NoteLoader {

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    NoteService noteService;

    @Autowired
    StudyService studyService;

    @Autowired
    VariantProvider variantProvider;

    @CrossOrigin
    @PostMapping("/note/delete/{id}")
    public void synchronizeSamples(@RequestBody AuthenticationDTO authenticationDTO, @PathVariable(value="id") long id) {
        User user = authenticationService.getUserForToken(authenticationDTO.tokenString);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no user found");
        }

        if (user.isAdmin()) {
            noteService.deleteNote(id);
        } else {
            noteService.deleteNoteByResearcher(id, user.getId());
        }
    }

    @CrossOrigin
    @PostMapping("/note/get/{sampleName}/{variantId}")
    public List<NoteDTO> getNotes(@RequestBody AuthenticationDTO authenticationDTO, @PathVariable(value="sampleName") String sampleName, @PathVariable(value="variantId") long variantId) {
        User user = authenticationService.getUserForToken(authenticationDTO.tokenString);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        if (!variantProvider.isValidSampleId(sampleName)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        if (!authenticationService.userCanAccessSample(user, sampleName)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        return noteService.getNotesForVariant(sampleName, variantId).stream()
                .map(Note::convertToDTO)
                .collect(Collectors.toList());
    }

    @CrossOrigin
    @PostMapping("/note/getForResearcher")
    public List<NoteDTO> getNotes(@RequestBody AuthenticationDTO authenticationDTO) {
        User user = authenticationService.getUserForToken(authenticationDTO.tokenString);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        List<Study> studies = studyService.getStudiesForUser(user);
        Set<Sample> sampleSet = new HashSet<>();
        for (Study study : studies) {
            sampleSet.addAll(studyService.getSamplesForStudy(study));
        }

        List<Note> notes = new LinkedList<>();
        for (Sample sample : sampleSet) {
            notes.addAll(noteService.getNotesForSample(sample.getName()));
        }

        return notes.stream().sorted((n1, n2) -> n2.getTime().compareTo(n1.getTime())).map(Note::convertToDTO).collect(Collectors.toList());
    }

    @CrossOrigin
    @PostMapping("/note/create")
    public void createStudy(@RequestBody CreateNoteDTO createNoteDTO) {
        User user = authenticationService.getUserForToken(createNoteDTO.tokenString);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no user found");
        }

        NoteDTO noteDTO = createNoteDTO.note;
        noteService.createNote(new Note(
                0,
                noteDTO.sampleId,
                noteDTO.sampleName,
                noteDTO.researcherId,
                noteDTO.researcherName,
                noteDTO.variantId,
                noteDTO.variantPosition,
                noteDTO.note,
                LocalDateTime.now()
        ));
    }

}
