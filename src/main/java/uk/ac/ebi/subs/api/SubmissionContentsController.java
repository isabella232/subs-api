package uk.ac.ebi.subs.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.helpers.SubmittableControllerSupport;
import uk.ac.ebi.subs.repository.model.*;

@RestController
@BasePathAwareController
@RequestMapping(value = "/submission/{submissionId}", method = RequestMethod.GET)
public class SubmissionContentsController {

    @Autowired
    SubmittableControllerSupport<Analysis> analysisControllerSupport;
    @Autowired
    SubmittableControllerSupport<Assay> assayControllerSupport;
    @Autowired
    SubmittableControllerSupport<AssayData> assayDataControllerSupport;
    @Autowired
    SubmittableControllerSupport<EgaDac> egaDacControllerSupport;
    @Autowired
    SubmittableControllerSupport<EgaDacPolicy> egaDacPolicyControllerSupport;
    @Autowired
    SubmittableControllerSupport<EgaDataset> egaDatasetControllerSupport;
    @Autowired
    SubmittableControllerSupport<Project> projectControllerSupport;
    @Autowired
    SubmittableControllerSupport<Protocol> protocolControllerSupport;
    @Autowired
    SubmittableControllerSupport<Sample> sampleControllerSupport;
    @Autowired
    SubmittableControllerSupport<SampleGroup> sampleGroupControllerSupport;
    @Autowired
    SubmittableControllerSupport<Study> studyControllerSupport;

    @RequestMapping("/analyses")
    public PagedResources<Resource<Analysis>> submissionAnalyses(
            @PathVariable String submissionId,
            Pageable pageable) {
        return analysisControllerSupport.submittablesInSubmission(submissionId,pageable);
    }

    @RequestMapping("/assays")
    public PagedResources<Resource<Assay>> submissionAssays(
            @PathVariable String submissionId,
            Pageable pageable) {
        return assayControllerSupport.submittablesInSubmission(submissionId,pageable);
    }

    @RequestMapping("/assayData")
    public PagedResources<Resource<AssayData>> submissionAssayData(
            @PathVariable String submissionId,
            Pageable pageable) {
        return assayDataControllerSupport.submittablesInSubmission(submissionId,pageable);
    }

    @RequestMapping("/egaDacs")
    public PagedResources<Resource<EgaDac>> submissionEgaDacs(
            @PathVariable String submissionId,
            Pageable pageable) {
        return egaDacControllerSupport.submittablesInSubmission(submissionId,pageable);
    }

    @RequestMapping("/egaDacPolicies")
    public PagedResources<Resource<EgaDacPolicy>> submissionEgaDacPolicies(
            @PathVariable String submissionId,
            Pageable pageable) {
        return egaDacPolicyControllerSupport.submittablesInSubmission(submissionId,pageable);
    }

    @RequestMapping("/egaDatasets")
    public PagedResources<Resource<EgaDataset>> submissionEgaDatasets(
            @PathVariable String submissionId,
            Pageable pageable) {
        return egaDatasetControllerSupport.submittablesInSubmission(submissionId,pageable);
    }

    @RequestMapping("/projects")
    public PagedResources<Resource<Project>> submissionProjects(
    @PathVariable String submissionId,
    Pageable pageable) {
        return projectControllerSupport.submittablesInSubmission(submissionId,pageable);
    }

    @RequestMapping("/protocols")
    public PagedResources<Resource<Protocol>> submissionProtocols(
    @PathVariable String submissionId,
    Pageable pageable) {
        return protocolControllerSupport.submittablesInSubmission(submissionId,pageable);
    }

    @RequestMapping("/samples")
    public PagedResources<Resource<Sample>> submissionSamples(
    @PathVariable String submissionId,
    Pageable pageable) {
        return sampleControllerSupport.submittablesInSubmission(submissionId,pageable);
    }

    @RequestMapping("/sampleGroups")
    public PagedResources<Resource<SampleGroup>> submissionSampleGroups(
    @PathVariable String submissionId,
    Pageable pageable) {
        return sampleGroupControllerSupport.submittablesInSubmission(submissionId,pageable);
    }

    @RequestMapping("/studies")
    public PagedResources<Resource<Study>> submissionStudies(
    @PathVariable String submissionId,
    Pageable pageable) {
        return studyControllerSupport.submittablesInSubmission(submissionId,pageable);
    }





}
