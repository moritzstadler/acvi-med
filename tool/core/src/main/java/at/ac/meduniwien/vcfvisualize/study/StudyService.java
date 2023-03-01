package at.ac.meduniwien.vcfvisualize.study;

import at.ac.meduniwien.vcfvisualize.data.MySqlLoader;
import at.ac.meduniwien.vcfvisualize.data.PostgresLoader;
import at.ac.meduniwien.vcfvisualize.data.discreetcolumnvalues.ColumnValuesProvider;
import at.ac.meduniwien.vcfvisualize.model.Sample;
import at.ac.meduniwien.vcfvisualize.model.Study;
import at.ac.meduniwien.vcfvisualize.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudyService {

    @Autowired
    PostgresLoader postgresLoader;

    @Autowired
    ColumnValuesProvider columnValuesProvider;

    @Autowired
    MySqlLoader mySqlLoader;

    public void synchronizeSamples() {
        List<String> datasetIds = postgresLoader.getSampleIds();

        for (String datasetId : datasetIds) {
            if (mySqlLoader.getSampleByName(datasetId) == null) {
                Sample sampleToBeCreated = new Sample(datasetId, "", "");
                mySqlLoader.createSample(sampleToBeCreated);
            }
        }

        //remove deleted samples from mysql
        Set<String> samples = mySqlLoader.getAllSamples().stream().map(Sample::getName).collect(Collectors.toSet());
        for (String datasetId : datasetIds) {
            samples.remove(datasetId);
        }

        for (String sampleToDelete : samples) {
            mySqlLoader.deleteSample(mySqlLoader.getSampleByName(sampleToDelete).getId());
            mySqlLoader.deleteSampleMeta(sampleToDelete);
        }

        columnValuesProvider.loadColumnValues();
    }

    public void deleteSample(String name) {
        postgresLoader.deleteSample(name);
    }

    public List<Sample> getAllSamples() {
        return mySqlLoader.getAllSamples();
    }

    public Sample getSampleByName(String name) {
        return mySqlLoader.getSampleByName(name);
    }

    public List<Study> getAllStudies() {
        return mySqlLoader.getAllStudies();
    }

    public List<Study> getStudiesForUser(User user) {
        return mySqlLoader.getStudiesForUser(user);
    }

    public List<Sample> getSamplesForStudy(Study study) {
        return mySqlLoader.getSamplesForStudy(study);
    }

    public void addStudyToUser(Study study, User user) {
        mySqlLoader.addStudyToUser(study, user);
    }

    public void addSampleToStudy(Sample sample, Study study) {
        mySqlLoader.addSampleToStudy(sample, study);
    }

    public void removeStudyFromUser(Study study, User user) {
        mySqlLoader.removeStudyFromUser(study, user);
    }

    public void removeSampleFromStudy(Sample sample, Study study) {
        mySqlLoader.removeSampleFromStudy(sample, study);
    }

    public void createStudy(Study study) {
        mySqlLoader.createStudy(study);
    }

    public void deleteStudy(Study study) {
        mySqlLoader.deleteStudy(study.getId());
    }

    public void updateSample(Sample sample) {
        mySqlLoader.updateSample(sample);
    }

}
