package uk.ac.ebi.subs.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.data.component.StudyDataType;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Component
@ConfigurationProperties("usi.studyDataType")
@Data
public class StudyDataTypeConfig {

    private boolean sequencingEnabled = true;
    private boolean sequencingAssemblyEnabled = true;
    private boolean sequencingVariationEnabled = true;
    private boolean sequencingSequenceAnnotation = true;

    private boolean functionalGenomicsEnabled = true;
    private boolean functionalGenomicsSequencing = true;
    private boolean functionalGenomicsArrays = true;

    private boolean metabolomicsEnabled = true;

    private boolean proteomicsEnabled = true;


    public Map<StudyDataType,Collection<String>> enabledDatatypes() {
        Map<StudyDataType,Collection<String>> enabledDataTypes = new TreeMap<>();

        if (sequencingEnabled) {
            Set<String> seqSubTypes = new TreeSet<>();
            if (sequencingAssemblyEnabled)
                seqSubTypes.add("Genome assemblies");

            if (sequencingSequenceAnnotation)
                seqSubTypes.add("Sequence annotation");

            if (sequencingVariationEnabled)
                seqSubTypes.add("Sequence variation");

            enabledDataTypes.put(StudyDataType.Sequencing, seqSubTypes);
        }

        if (metabolomicsEnabled) {
            enabledDataTypes.put(StudyDataType.Metabolomics, Collections.emptySet());
        }

        if (functionalGenomicsEnabled) {
            Set<String> seqSubTypes = new TreeSet<>();
            if (functionalGenomicsArrays)
                seqSubTypes.add("Microarrays");

            if (functionalGenomicsSequencing)
                seqSubTypes.add("Sequencing");

            enabledDataTypes.put(StudyDataType.FunctionalGenomics, seqSubTypes);
        }

        if (proteomicsEnabled) {
            enabledDataTypes.put(StudyDataType.Proteomics, Collections.emptySet());
        }

        return enabledDataTypes;
    }
}

