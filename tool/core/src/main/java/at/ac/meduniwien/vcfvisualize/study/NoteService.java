package at.ac.meduniwien.vcfvisualize.study;

import at.ac.meduniwien.vcfvisualize.data.MySqlLoader;
import at.ac.meduniwien.vcfvisualize.data.PostgresLoader;
import at.ac.meduniwien.vcfvisualize.data.discreetcolumnvalues.ColumnValuesProvider;
import at.ac.meduniwien.vcfvisualize.model.Note;
import at.ac.meduniwien.vcfvisualize.model.Sample;
import at.ac.meduniwien.vcfvisualize.model.Study;
import at.ac.meduniwien.vcfvisualize.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NoteService {

    @Autowired
    MySqlLoader mySqlLoader;

    public void deleteNote(long id) {
        mySqlLoader.deleteNote(id);
    }

    public void deleteNoteByResearcher(long noteId, long researcherId) {
        mySqlLoader.deleteNote(noteId, researcherId);
    }

    public List<Note> getNotesForVariant(String sampleName, long variantId) {
        return mySqlLoader.getNotesForVariant(sampleName, variantId);
    }

    public List<Note> getNotesForSample(String sampleName) {
        return mySqlLoader.getNotesBySampleName(sampleName);
    }

    public void createNote(Note note) {
        mySqlLoader.createNote(note);
    }

}
