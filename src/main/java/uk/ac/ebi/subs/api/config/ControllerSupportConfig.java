package uk.ac.ebi.subs.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import uk.ac.ebi.subs.api.helpers.SubmittableControllerSupport;
import uk.ac.ebi.subs.repository.model.*;
import uk.ac.ebi.subs.repository.repos.*;

@Configuration
public class ControllerSupportConfig {

    @Bean
    public SubmittableControllerSupport<Analysis> analysisControllerSupport(
            AnalysisRepository analysisRepository,
            PagedResourcesAssembler<Analysis> pagedResourcesAssembler,
            ResourceProcessor<Resource<Analysis>> analysisResourceProcessor,
            EntityLinks entityLinks
    ) {

        return new SubmittableControllerSupport<Analysis>(
                analysisRepository,
                pagedResourcesAssembler,
                analysisResourceProcessor,
                entityLinks
        );
    }

    @Bean
    public SubmittableControllerSupport<Assay> assayControllerSupport(
            AssayRepository assayRepository,
            PagedResourcesAssembler<Assay> pagedResourcesAssembler,
            ResourceProcessor<Resource<Assay>> assayResourceProcessor,
            EntityLinks entityLinks
    ) {

        return new SubmittableControllerSupport<Assay>(
                assayRepository,
                pagedResourcesAssembler,
                assayResourceProcessor,
                entityLinks
        );
    }

    @Bean
    public SubmittableControllerSupport<AssayData> assayDataControllerSupport(
            AssayDataRepository assayDataRepository,
            PagedResourcesAssembler<AssayData> pagedResourcesAssembler,
            ResourceProcessor<Resource<AssayData>> assayDataResourceProcessor,
            EntityLinks entityLinks
    ) {

        return new SubmittableControllerSupport<AssayData>(
                assayDataRepository,
                pagedResourcesAssembler,
                assayDataResourceProcessor,
                entityLinks
        );
    }

    @Bean
    public SubmittableControllerSupport<EgaDac> egaDacControllerSupport(
            EgaDacRepository egaDacRepository,
            PagedResourcesAssembler<EgaDac> pagedResourcesAssembler,
            ResourceProcessor<Resource<EgaDac>> egaDacResourceProcessor,
            EntityLinks entityLinks
    ) {

        return new SubmittableControllerSupport<EgaDac>(
                egaDacRepository,
                pagedResourcesAssembler,
                egaDacResourceProcessor,
                entityLinks
        );
    }

    @Bean
    public SubmittableControllerSupport<EgaDacPolicy> egaDacPolicyControllerSupport(
            EgaDacPolicyRepository egaDacPolicyRepository,
            PagedResourcesAssembler<EgaDacPolicy> pagedResourcesAssembler,
            ResourceProcessor<Resource<EgaDacPolicy>> egaDacPolicyResourceProcessor,
            EntityLinks entityLinks
    ) {

        return new SubmittableControllerSupport<EgaDacPolicy>(
                egaDacPolicyRepository,
                pagedResourcesAssembler,
                egaDacPolicyResourceProcessor,
                entityLinks
        );
    }

    @Bean
    public SubmittableControllerSupport<EgaDataset> egaDatasetControllerSupport(
            EgaDatasetRepository egaDatasetRepository,
            PagedResourcesAssembler<EgaDataset> pagedResourcesAssembler,
            ResourceProcessor<Resource<EgaDataset>> egaDatasetResourceProcessor,
            EntityLinks entityLinks
    ) {

        return new SubmittableControllerSupport<EgaDataset>(
                egaDatasetRepository,
                pagedResourcesAssembler,
                egaDatasetResourceProcessor,
                entityLinks
        );
    }

    @Bean
    public SubmittableControllerSupport<Project> projectControllerSupport(
            ProjectRepository projectRepository,
            PagedResourcesAssembler<Project> pagedResourcesAssembler,
            ResourceProcessor<Resource<Project>> projectResourceProcessor,
            EntityLinks entityLinks
    ) {

        return new SubmittableControllerSupport<Project>(
                projectRepository,
                pagedResourcesAssembler,
                projectResourceProcessor,
                entityLinks
        );
    }

    @Bean
    public SubmittableControllerSupport<Protocol> protocolControllerSupport(
            ProtocolRepository protocolRepository,
            PagedResourcesAssembler<Protocol> pagedResourcesAssembler,
            ResourceProcessor<Resource<Protocol>> protocolResourceProcessor,
            EntityLinks entityLinks
    ) {

        return new SubmittableControllerSupport<Protocol>(
                protocolRepository,
                pagedResourcesAssembler,
                protocolResourceProcessor,
                entityLinks
        );
    }

    @Bean
    public SubmittableControllerSupport<Sample> sampleControllerSupport(
            SampleRepository sampleRepository,
            PagedResourcesAssembler<Sample> pagedResourcesAssembler,
            ResourceProcessor<Resource<Sample>> sampleResourceProcessor,
            EntityLinks entityLinks
    ) {

        return new SubmittableControllerSupport<Sample>(
                sampleRepository,
                pagedResourcesAssembler,
                sampleResourceProcessor,
                entityLinks
        );
    }

    @Bean
    public SubmittableControllerSupport<SampleGroup> sampleGroupControllerSupport(
            SampleGroupRepository sampleGroupRepository,
            PagedResourcesAssembler<SampleGroup> pagedResourcesAssembler,
            ResourceProcessor<Resource<SampleGroup>> sampleGroupResourceProcessor,
            EntityLinks entityLinks
    ) {

        return new SubmittableControllerSupport<SampleGroup>(
                sampleGroupRepository,
                pagedResourcesAssembler,
                sampleGroupResourceProcessor,
                entityLinks
        );
    }

    @Bean
    public SubmittableControllerSupport<Study> studyControllerSupport(
            StudyRepository studyRepository,
            PagedResourcesAssembler<Study> pagedResourcesAssembler,
            ResourceProcessor<Resource<Study>> studyResourceProcessor,
            EntityLinks entityLinks
    ) {

        return new SubmittableControllerSupport<Study>(
                studyRepository,
                pagedResourcesAssembler,
                studyResourceProcessor,
                entityLinks
        );
    }

}
