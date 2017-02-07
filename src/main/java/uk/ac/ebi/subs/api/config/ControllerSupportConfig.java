package uk.ac.ebi.subs.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PagedResourcesAssembler;
import uk.ac.ebi.subs.api.helpers.SubmittableControllerSupport;
import uk.ac.ebi.subs.api.resourceAssembly.SimpleResourceAssembler;
import uk.ac.ebi.subs.repository.model.*;
import uk.ac.ebi.subs.repository.repos.*;

@Configuration
public class ControllerSupportConfig {

    @Bean
    public SubmittableControllerSupport<Analysis> analysisControllerSupport(
            AnalysisRepository analysisRepository,
            PagedResourcesAssembler<Analysis> pagedResourcesAssembler,
            SimpleResourceAssembler<Analysis> simpleResourceAssembler
    ) {

        return new SubmittableControllerSupport<Analysis>(
                analysisRepository,
                pagedResourcesAssembler,
                simpleResourceAssembler
        );
    }

    @Bean
    public SubmittableControllerSupport<Assay> assayControllerSupport(
            AssayRepository assayRepository,
            PagedResourcesAssembler<Assay> pagedResourcesAssembler,
            SimpleResourceAssembler<Assay> simpleResourceAssembler
    ) {

        return new SubmittableControllerSupport<Assay>(
                assayRepository,
                pagedResourcesAssembler,
                simpleResourceAssembler
        );
    }

    @Bean
    public SubmittableControllerSupport<AssayData> assayDataControllerSupport(
            AssayDataRepository assayDataRepository,
            PagedResourcesAssembler<AssayData> pagedResourcesAssembler,
            SimpleResourceAssembler<AssayData> simpleResourceAssembler
    ) {

        return new SubmittableControllerSupport<AssayData>(
                assayDataRepository,
                pagedResourcesAssembler,
                simpleResourceAssembler
        );
    }

    @Bean
    public SubmittableControllerSupport<EgaDac> egaDacControllerSupport(
            EgaDacRepository egaDacRepository,
            PagedResourcesAssembler<EgaDac> pagedResourcesAssembler,
            SimpleResourceAssembler<EgaDac> simpleResourceAssembler
    ) {

        return new SubmittableControllerSupport<EgaDac>(
                egaDacRepository,
                pagedResourcesAssembler,
                simpleResourceAssembler
        );
    }

    @Bean
    public SubmittableControllerSupport<EgaDacPolicy> egaDacPolicyControllerSupport(
            EgaDacPolicyRepository egaDacPolicyRepository,
            PagedResourcesAssembler<EgaDacPolicy> pagedResourcesAssembler,
            SimpleResourceAssembler<EgaDacPolicy> simpleResourceAssembler
    ) {

        return new SubmittableControllerSupport<EgaDacPolicy>(
                egaDacPolicyRepository,
                pagedResourcesAssembler,
                simpleResourceAssembler
        );
    }

    @Bean
    public SubmittableControllerSupport<EgaDataset> egaDatasetControllerSupport(
            EgaDatasetRepository egaDatasetRepository,
            PagedResourcesAssembler<EgaDataset> pagedResourcesAssembler,
            SimpleResourceAssembler<EgaDataset> simpleResourceAssembler
    ) {

        return new SubmittableControllerSupport<EgaDataset>(
                egaDatasetRepository,
                pagedResourcesAssembler,
                simpleResourceAssembler
        );
    }

    @Bean
    public SubmittableControllerSupport<Project> projectControllerSupport(
            ProjectRepository projectRepository,
            PagedResourcesAssembler<Project> pagedResourcesAssembler,
            SimpleResourceAssembler<Project> simpleResourceAssembler
    ) {

        return new SubmittableControllerSupport<Project>(
                projectRepository,
                pagedResourcesAssembler,
                simpleResourceAssembler
        );
    }

    @Bean
    public SubmittableControllerSupport<Protocol> protocolControllerSupport(
            ProtocolRepository protocolRepository,
            PagedResourcesAssembler<Protocol> pagedResourcesAssembler,
            SimpleResourceAssembler<Protocol> simpleResourceAssembler
    ) {

        return new SubmittableControllerSupport<Protocol>(
                protocolRepository,
                pagedResourcesAssembler,
                simpleResourceAssembler
        );
    }

    @Bean
    public SubmittableControllerSupport<Sample> sampleControllerSupport(
            SampleRepository sampleRepository,
            PagedResourcesAssembler<Sample> pagedResourcesAssembler,
            SimpleResourceAssembler<Sample> simpleResourceAssembler
    ) {

        return new SubmittableControllerSupport<Sample>(
                sampleRepository,
                pagedResourcesAssembler,
                simpleResourceAssembler
        );
    }

    @Bean
    public SubmittableControllerSupport<SampleGroup> sampleGroupControllerSupport(
            SampleGroupRepository sampleGroupRepository,
            PagedResourcesAssembler<SampleGroup> pagedResourcesAssembler,
            SimpleResourceAssembler<SampleGroup> simpleResourceAssembler
    ) {

        return new SubmittableControllerSupport<SampleGroup>(
                sampleGroupRepository,
                pagedResourcesAssembler,
                simpleResourceAssembler
        );
    }

    @Bean
    public SubmittableControllerSupport<Study> studyControllerSupport(
            StudyRepository studyRepository,
            PagedResourcesAssembler<Study> pagedResourcesAssembler,
            SimpleResourceAssembler<Study> simpleResourceAssembler
    ) {

        return new SubmittableControllerSupport<Study>(
                studyRepository,
                pagedResourcesAssembler,
                simpleResourceAssembler
        );
    }

}
