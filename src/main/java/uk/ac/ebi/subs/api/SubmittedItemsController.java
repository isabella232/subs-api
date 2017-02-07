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
    public Resource<Analysis> analysisLatestVersion(@PathVariable String domainName, @PathVariable String alias){
        return analysisControllerSupport.submittableLatestVersion(domainName,alias);
    }

    @RequestMapping("/analysis/{alias}/history")
    public PagedResources<Resource<Analysis>> analysisSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return analysisControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/assays/{alias}")
    public Resource<Assay> assayLatestVersion(@PathVariable String domainName, @PathVariable String alias){
        return assayControllerSupport.submittableLatestVersion(domainName,alias);
    }

    @RequestMapping("/assays/{alias}/history")
    public PagedResources<Resource<Assay>> assaySubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return assayControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/assayData/{alias}")
    public Resource<AssayData> assayDataLatestVersion(@PathVariable String domainName, @PathVariable String alias){
        return assayDataControllerSupport.submittableLatestVersion(domainName,alias);
    }

    @RequestMapping("/assayData/{alias}/history")
    public PagedResources<Resource<AssayData>> assayDataSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return assayDataControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/egaDacs/{alias}")
    public Resource<EgaDac> egaDacLatestVersion(@PathVariable String domainName, @PathVariable String alias){
        return egaDacControllerSupport.submittableLatestVersion(domainName,alias);
    }

    @RequestMapping("/egaDacs/{alias}/history")
    public PagedResources<Resource<EgaDac>> egaDacSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return egaDacControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/egaDacPolicies/{alias}")
    public Resource<EgaDacPolicy> egaDacPolicyLatestVersion(@PathVariable String domainName, @PathVariable String alias){
        return egaDacPolicyControllerSupport.submittableLatestVersion(domainName,alias);
    }

    @RequestMapping("/egaDacPolicies/{alias}/history")
    public PagedResources<Resource<EgaDacPolicy>> egaDacPolicySubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return egaDacPolicyControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/egaDatasets/{alias}")
    public Resource<EgaDataset> egaDatasetLatestVersion(@PathVariable String domainName, @PathVariable String alias){
        return egaDatasetControllerSupport.submittableLatestVersion(domainName,alias);
    }

    @RequestMapping("/egaDatasets/{alias}/history")
    public PagedResources<Resource<EgaDataset>> egaDatasetSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return egaDatasetControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/projects/{alias}")
    public Resource<Project> projectLatestVersion(@PathVariable String domainName, @PathVariable String alias){
        return projectControllerSupport.submittableLatestVersion(domainName,alias);
    }

    @RequestMapping("/projects/{alias}/history")
    public PagedResources<Resource<Project>> projectSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return projectControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/protocols/{alias}")
    public Resource<Protocol> protocolLatestVersion(@PathVariable String domainName, @PathVariable String alias){
        return protocolControllerSupport.submittableLatestVersion(domainName,alias);
    }

    @RequestMapping("/protocols/{alias}/history")
    public PagedResources<Resource<Protocol>> protocolSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return protocolControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/samples/{alias}")
    public Resource<Sample> sampleLatestVersion(@PathVariable String domainName, @PathVariable String alias){
        return sampleControllerSupport.submittableLatestVersion(domainName,alias);
    }

    @RequestMapping("/samples/{alias}/history")
    public PagedResources<Resource<Sample>> sampleSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return sampleControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/sampleGroups/{alias}")
    public Resource<SampleGroup> sampleGroupLatestVersion(@PathVariable String domainName, @PathVariable String alias){
        return sampleGroupControllerSupport.submittableLatestVersion(domainName,alias);
    }

    @RequestMapping("/sampleGroups/{alias}/history")
    public PagedResources<Resource<SampleGroup>> sampleGroupSubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return sampleGroupControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }

    @RequestMapping("/studies/{alias}")
    public Resource<Study> studyLatestVersion(@PathVariable String domainName, @PathVariable String alias){
        return studyControllerSupport.submittableLatestVersion(domainName,alias);
    }

    @RequestMapping("/studies/{alias}/history")
    public PagedResources<Resource<Study>> studySubmissionHistory(@PathVariable String domainName, @PathVariable String alias, Pageable pageable){
        return studyControllerSupport.submittableSubmissionHistory(domainName,alias,pageable);
    }





}
