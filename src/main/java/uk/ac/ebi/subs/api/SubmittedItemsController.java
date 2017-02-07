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
@RequestMapping("/domains/{domainName}")
public class SubmittedItemsController {

    @Autowired
    private SubmittableControllerSupport<Analysis> analysisControllerSupport;
    @Autowired
    private SubmittableControllerSupport<Assay> assayControllerSupport;
    @Autowired
    private SubmittableControllerSupport<AssayData> assayDataControllerSupport;
    @Autowired
    private SubmittableControllerSupport<EgaDac> egaDacControllerSupport;
    @Autowired
    private SubmittableControllerSupport<EgaDacPolicy> egaDacPolicyControllerSupport;
    @Autowired
    private SubmittableControllerSupport<EgaDataset> egaDatasetControllerSupport;
    @Autowired
    private SubmittableControllerSupport<Project> projectControllerSupport;
    @Autowired
    private SubmittableControllerSupport<Protocol> protocolControllerSupport;
    @Autowired
    private SubmittableControllerSupport<Sample> sampleControllerSupport;
    @Autowired
    private SubmittableControllerSupport<SampleGroup> sampleGroupControllerSupport;
    @Autowired
    private SubmittableControllerSupport<Study> studyControllerSupport;

    @RequestMapping("/analysis/{alias}")
    public PagedResources<Resource<Analysis>> analysisSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return analysisControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/assays/{alias}")
    public PagedResources<Resource<Assay>> assaySubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return assayControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/assayData/{alias}")
    public PagedResources<Resource<AssayData>> assayDataSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return assayDataControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/egaDacs/{alias}")
    public PagedResources<Resource<EgaDac>> egaDacSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return egaDacControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/egaDacPolicies/{alias}")
    public PagedResources<Resource<EgaDacPolicy>> egaDacPolicySubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return egaDacPolicyControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/egaDatasets/{alias}")
    public PagedResources<Resource<EgaDataset>> egaDatasetSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return egaDatasetControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/projects/{alias}")
    public PagedResources<Resource<Project>> projectSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return projectControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/protocols/{alias}")
    public PagedResources<Resource<Protocol>> protocolSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return protocolControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/samples/{alias}")
    public PagedResources<Resource<Sample>> sampleSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return sampleControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/sampleGroups/{alias}")
    public PagedResources<Resource<SampleGroup>> sampleGroupSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return sampleGroupControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/studies/{alias}")
    public PagedResources<Resource<Study>> studySubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return studyControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }





}
