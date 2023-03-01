package at.ac.meduniwien.vcfvisualize.cron;

import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.Clinvar;
import at.ac.meduniwien.vcfvisualize.knowledgebase.hpo.Hpo;
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
    Hpo hpo;

    @Autowired
    Clinvar clinvar;

    @Autowired
    StudyService studyService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void performCronJobs() {
        new Thread(() -> studyService.synchronizeSamples()).start();

        new Thread(() -> {
            panelApp.loadDataFromAPI();
            hpo.loadDataFromAPI();
        }).start();

        new Thread(() -> clinvar.initiate()).start();
    }

    @PostConstruct
    public void init() {
        new Thread(() -> studyService.synchronizeSamples()).start();

        new Thread(() -> {
            panelApp.loadDataFromAPI();
            hpo.loadDataFromAPI();
        }).start();

        new Thread(() -> clinvar.initiate()).start();
    }

}
