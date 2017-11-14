package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.config.StudyDataTypeConfig;
import uk.ac.ebi.subs.data.component.StudyDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@RestController
@BasePathAwareController
public class StudyDataTypeController {

    private StudyDataTypeConfig studyDataTypeConfig;

    public StudyDataTypeController(StudyDataTypeConfig studyDataTypeConfig) {
        this.studyDataTypeConfig = studyDataTypeConfig;
    }

    @RequestMapping("/studyDataTypes")
    public Collection<String> getStudyDataTypes(){
        return studyDataTypeConfig.enabledDatatypes().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
