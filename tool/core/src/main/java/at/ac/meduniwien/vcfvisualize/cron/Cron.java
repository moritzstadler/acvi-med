package at.ac.meduniwien.vcfvisualize.cron;

import at.ac.meduniwien.vcfvisualize.data.discreetcolumnvalues.ColumnValuesProvider;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.PanelApp;
import at.ac.meduniwien.vcfvisualize.study.StudyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;

@Configuration
@EnableScheduling
public class Cron {

    @Autowired
    PanelApp panelApp;

    @Autowired
    StudyService studyService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void performCronJobs() {
        new Thread(() -> studyService.synchronizeSamples()).start();
        new Thread(() -> panelApp.loadDataFromAPI()).start();
    }

    @PostConstruct
    public void init() {
        //load all samples from big query
        new Thread(() -> studyService.synchronizeSamples()).start();

        //load data from panel app api
        new Thread(() -> panelApp.loadDataFromAPI()).start();
    }

}
