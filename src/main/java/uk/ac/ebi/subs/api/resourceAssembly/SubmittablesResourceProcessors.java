package uk.ac.ebi.subs.api.resourceAssembly;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import uk.ac.ebi.subs.api.SubmissionContentsController;
import uk.ac.ebi.subs.api.SubmittedItemsController;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.SubmissionLinks;
import uk.ac.ebi.subs.repository.model.*;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by davidr on 20/01/2017.
 */
@Configuration
public class SubmittablesResourceProcessors {


    private Pageable defaultPageRequest() {
        return new PageRequest(0, 1);
    }

    private Class<SubmissionContentsController> submittablesControllerClass = SubmissionContentsController.class;

    @Bean
    public ResourceProcessor<Resource<Submission>> submissionProcessor() {

        return new ResourceProcessor<Resource<Submission>>() {

            @Override
            public Resource<Submission> process(Resource<Submission> resource) {

                resource.add(
                        linkTo(
                                methodOn(submittablesControllerClass)
                                        .submissionAnalyses(
                                                resource.getContent().getId(),
                                                defaultPageRequest()
                                        ))
                                .withRel(SubmissionLinks.ANALYSIS)
                );

                resource.add(
                        linkTo(
                                methodOn(submittablesControllerClass)
                                        .submissionAssays(
                                                resource.getContent().getId(),
                                                defaultPageRequest()
                                        ))
                                .withRel(SubmissionLinks.ASSAY)
                );

                resource.add(
                        linkTo(
                                methodOn(submittablesControllerClass)
                                        .submissionAssayData(
                                                resource.getContent().getId(),
                                                defaultPageRequest()
                                        ))
                                .withRel(SubmissionLinks.ASSAY_DATA)
                );

                resource.add(
                        linkTo(
                                methodOn(submittablesControllerClass)
                                        .submissionEgaDacs(
                                                resource.getContent().getId(),
                                                defaultPageRequest()
                                        ))
                                .withRel(SubmissionLinks.EGA_DAC)
                );

                resource.add(
                        linkTo(
                                methodOn(submittablesControllerClass)
                                        .submissionEgaDacPolicies(
                                                resource.getContent().getId(),
                                                defaultPageRequest()
                                        ))
                                .withRel(SubmissionLinks.EGA_DAC_POLICY)
                );

                resource.add(
                        linkTo(
                                methodOn(submittablesControllerClass)
                                        .submissionEgaDatasets(
                                                resource.getContent().getId(),
                                                defaultPageRequest()
                                        ))
                                .withRel(SubmissionLinks.EGA_DATASET)
                );

                resource.add(
                        linkTo(
                                methodOn(submittablesControllerClass)
                                        .submissionProjects(
                                                resource.getContent().getId(),
                                                defaultPageRequest()
                                        ))
                                .withRel(SubmissionLinks.PROJECT)
                );

                resource.add(
                        linkTo(
                                methodOn(submittablesControllerClass)
                                        .submissionProtocols(
                                                resource.getContent().getId(),
                                                defaultPageRequest()
                                        ))
                                .withRel(SubmissionLinks.PROTOCOL)
                );

                resource.add(
                        linkTo(
                                methodOn(submittablesControllerClass)
                                        .submissionSamples(
                                                resource.getContent().getId(),
                                                defaultPageRequest()
                                        ))
                                .withRel(SubmissionLinks.SAMPLE)
                );

                resource.add(
                        linkTo(
                                methodOn(submittablesControllerClass)
                                        .submissionSampleGroups(
                                                resource.getContent().getId(),
                                                defaultPageRequest()
                                        ))
                                .withRel(SubmissionLinks.SAMPLE_GROUP)
                );

                resource.add(
                        linkTo(
                                methodOn(submittablesControllerClass)
                                        .submissionStudies(
                                                resource.getContent().getId(),
                                                defaultPageRequest()
                                        ))
                                .withRel(SubmissionLinks.STUDY)
                );

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<Analysis>> analysisProcessor() {

        return new ResourceProcessor<Resource<Analysis>>() {

            @Override
            public Resource<Analysis> process(Resource<Analysis> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).analysisSubmissionHistory(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias(),
                                            defaultPageRequest()
                                    )
                            ).withRel(SubmissionLinks.HISTORY)
                    );

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).analysisLatestVersion(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias()
                                    )
                            ).withRel(SubmissionLinks.LATEST_VERSION)
                    );
                }

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<Assay>> assayProcessor() {

        return new ResourceProcessor<Resource<Assay>>() {

            @Override
            public Resource<Assay> process(Resource<Assay> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).assaySubmissionHistory(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias(),
                                            defaultPageRequest()
                                    )
                            ).withRel(SubmissionLinks.HISTORY)
                    );

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).assayLatestVersion(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias()
                                    )
                            ).withRel(SubmissionLinks.LATEST_VERSION)
                    );
                }

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<AssayData>> assayDataProcessor() {

        return new ResourceProcessor<Resource<AssayData>>() {

            @Override
            public Resource<AssayData> process(Resource<AssayData> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).assayDataSubmissionHistory(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias(),
                                            defaultPageRequest()
                                    )
                            ).withRel(SubmissionLinks.HISTORY)
                    );

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).assayDataLatestVersion(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias()
                                    )
                            ).withRel(SubmissionLinks.LATEST_VERSION)
                    );
                }


                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<EgaDac>> egaDacProcessor() {

        return new ResourceProcessor<Resource<EgaDac>>() {

            @Override
            public Resource<EgaDac> process(Resource<EgaDac> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).egaDacSubmissionHistory(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias(),
                                            defaultPageRequest()
                                    )
                            ).withRel(SubmissionLinks.HISTORY)
                    );

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).egaDacLatestVersion(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias()
                                    )
                            ).withRel(SubmissionLinks.LATEST_VERSION)
                    );
                }


                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<EgaDacPolicy>> egaDacPolicyProcessor() {

        return new ResourceProcessor<Resource<EgaDacPolicy>>() {

            @Override
            public Resource<EgaDacPolicy> process(Resource<EgaDacPolicy> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).egaDacPolicySubmissionHistory(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias(),
                                            defaultPageRequest()
                                    )
                            ).withRel(SubmissionLinks.HISTORY)
                    );

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).egaDacPolicyLatestVersion(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias()
                                    )
                            ).withRel(SubmissionLinks.LATEST_VERSION)
                    );
                }


                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<EgaDataset>> egaDatasetProcessor() {

        return new ResourceProcessor<Resource<EgaDataset>>() {

            @Override
            public Resource<EgaDataset> process(Resource<EgaDataset> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).egaDatasetSubmissionHistory(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias(),
                                            defaultPageRequest()
                                    )
                            ).withRel(SubmissionLinks.HISTORY)
                    );

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).egaDatasetLatestVersion(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias()
                                    )
                            ).withRel(SubmissionLinks.LATEST_VERSION)
                    );
                }

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<Project>> projectProcessor() {

        return new ResourceProcessor<Resource<Project>>() {

            @Override
            public Resource<Project> process(Resource<Project> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).projectSubmissionHistory(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias(),
                                            defaultPageRequest()
                                    )
                            ).withRel(SubmissionLinks.HISTORY)
                    );

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).projectLatestVersion(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias()
                                    )
                            ).withRel(SubmissionLinks.LATEST_VERSION)
                    );
                }

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<Protocol>> protocolProcessor() {

        return new ResourceProcessor<Resource<Protocol>>() {

            @Override
            public Resource<Protocol> process(Resource<Protocol> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).protocolSubmissionHistory(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias(),
                                            defaultPageRequest()
                                    )
                            ).withRel(SubmissionLinks.HISTORY)
                    );

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).protocolLatestVersion(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias()
                                    )
                            ).withRel(SubmissionLinks.LATEST_VERSION)
                    );
                }

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<Sample>> sampleProcessor() {

        return new ResourceProcessor<Resource<Sample>>() {

            @Override
            public Resource<Sample> process(Resource<Sample> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).sampleSubmissionHistory(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias(),
                                            defaultPageRequest()
                                    )
                            ).withRel(SubmissionLinks.HISTORY)
                    );

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).sampleLatestVersion(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias()
                                    )
                            ).withRel(SubmissionLinks.LATEST_VERSION)
                    );
                }


                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<SampleGroup>> sampleGroupProcessor() {

        return new ResourceProcessor<Resource<SampleGroup>>() {

            @Override
            public Resource<SampleGroup> process(Resource<SampleGroup> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).sampleGroupSubmissionHistory(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias(),
                                            defaultPageRequest()
                                    )
                            ).withRel(SubmissionLinks.HISTORY)
                    );

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).sampleGroupLatestVersion(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias()
                                    )
                            ).withRel(SubmissionLinks.LATEST_VERSION)
                    );
                }

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<Study>> studyProcessor() {

        return new ResourceProcessor<Resource<Study>>() {

            @Override
            public Resource<Study> process(Resource<Study> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).studySubmissionHistory(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias(),
                                            defaultPageRequest()
                                    )
                            ).withRel(SubmissionLinks.HISTORY)
                    );

                    resource.add(
                            linkTo(
                                    methodOn(SubmittedItemsController.class).studyLatestVersion(
                                            resource.getContent().getDomain().getName(),
                                            resource.getContent().getAlias()
                                    )
                            ).withRel(SubmissionLinks.LATEST_VERSION)
                    );
                }

                return resource;
            }
        };
    }
}