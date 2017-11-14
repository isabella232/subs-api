package uk.ac.ebi.subs.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.data.component.StudyDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Component
@ConfigurationProperties("usi.studyDataType")
public class StudyDataTypeConfig {

    private boolean sequencingEnabled = true;
    private boolean metabolomicsEnabled = true;
    private boolean functionGenomicsEnabled = true;
    private boolean proteomicsEnabled = true;

    public Collection<StudyDataType> enabledDatatypes() {
        Set<StudyDataType> enabledDataTypes = new TreeSet<>();

        if (sequencingEnabled)
            enabledDataTypes.add(StudyDataType.Sequencing);

        if (metabolomicsEnabled) {
            enabledDataTypes.add(StudyDataType.Metabolomics);
        }

        if (functionGenomicsEnabled)
            enabledDataTypes.add(StudyDataType.FunctionalGenomics);

        if (proteomicsEnabled) {
            enabledDataTypes.add(StudyDataType.Proteomics);
        }

        return enabledDataTypes;
    }
}

