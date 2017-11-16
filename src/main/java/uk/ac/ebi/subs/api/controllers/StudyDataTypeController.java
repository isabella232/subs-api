package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.config.StudyDataTypeConfig;
import uk.ac.ebi.subs.data.component.StudyDataType;

import java.util.Collection;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@BasePathAwareController
public class StudyDataTypeController {

    private StudyDataTypeConfig studyDataTypeConfig;

    public StudyDataTypeController(StudyDataTypeConfig studyDataTypeConfig) {
        this.studyDataTypeConfig = studyDataTypeConfig;
    }

    @RequestMapping("/studyDataTypes")
    public Resource<Map<StudyDataType, Collection<String>>> getStudyDataTypes() {
        Link self = linkTo(methodOn(this.getClass()).getStudyDataTypes()).withSelfRel();

        return new Resource<>(studyDataTypeConfig.enabledDatatypes(), self);
    }
}
