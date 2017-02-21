package uk.ac.ebi.subs.api.resourceAssembly;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.util.Assert;
import uk.ac.ebi.subs.repository.model.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SubmittablesResourceProcessors {


    @Bean
    public ResourceProcessor<Resource<Analysis>> analysisProcessor(RepositoryEntityLinks repositoryEntityLinks) {

        return new ResourceProcessor<Resource<Analysis>>() {

            @Override
            public Resource<Analysis> process(Resource<Analysis> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    addHistory(resource, repositoryEntityLinks);
                    addCurrentVersion(resource, repositoryEntityLinks);


                }

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<Assay>> assayProcessor(RepositoryEntityLinks repositoryEntityLinks) {

        return new ResourceProcessor<Resource<Assay>>() {

            @Override
            public Resource<Assay> process(Resource<Assay> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    addHistory(resource, repositoryEntityLinks);
                    addCurrentVersion(resource, repositoryEntityLinks);


                }

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<AssayData>> assayDataProcessor(RepositoryEntityLinks repositoryEntityLinks) {

        return new ResourceProcessor<Resource<AssayData>>() {

            @Override
            public Resource<AssayData> process(Resource<AssayData> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    addHistory(resource, repositoryEntityLinks);
                    addCurrentVersion(resource, repositoryEntityLinks);


                }


                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<EgaDac>> egaDacProcessor(RepositoryEntityLinks repositoryEntityLinks) {

        return new ResourceProcessor<Resource<EgaDac>>() {

            @Override
            public Resource<EgaDac> process(Resource<EgaDac> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    addHistory(resource, repositoryEntityLinks);
                    addCurrentVersion(resource, repositoryEntityLinks);


                }


                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<EgaDacPolicy>> egaDacPolicyProcessor(RepositoryEntityLinks repositoryEntityLinks) {

        return new ResourceProcessor<Resource<EgaDacPolicy>>() {

            @Override
            public Resource<EgaDacPolicy> process(Resource<EgaDacPolicy> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    addHistory(resource, repositoryEntityLinks);
                    addCurrentVersion(resource, repositoryEntityLinks);


                }


                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<EgaDataset>> egaDatasetProcessor(RepositoryEntityLinks repositoryEntityLinks) {

        return new ResourceProcessor<Resource<EgaDataset>>() {

            @Override
            public Resource<EgaDataset> process(Resource<EgaDataset> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    addHistory(resource, repositoryEntityLinks);
                    addCurrentVersion(resource, repositoryEntityLinks);


                }

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<Project>> projectProcessor(RepositoryEntityLinks repositoryEntityLinks) {

        return new ResourceProcessor<Resource<Project>>() {

            @Override
            public Resource<Project> process(Resource<Project> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    addHistory(resource, repositoryEntityLinks);
                    addCurrentVersion(resource, repositoryEntityLinks);

                }

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<Protocol>> protocolProcessor(RepositoryEntityLinks repositoryEntityLinks) {

        return new ResourceProcessor<Resource<Protocol>>() {

            @Override
            public Resource<Protocol> process(Resource<Protocol> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    addHistory(resource, repositoryEntityLinks);
                    addCurrentVersion(resource, repositoryEntityLinks);


                }

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<Sample>> sampleProcessor(RepositoryEntityLinks repositoryEntityLinks) {

        return new ResourceProcessor<Resource<Sample>>() {

            @Override
            public Resource<Sample> process(Resource<Sample> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    addHistory(resource, repositoryEntityLinks);
                    addCurrentVersion(resource, repositoryEntityLinks);


                }


                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<SampleGroup>> sampleGroupProcessor(RepositoryEntityLinks repositoryEntityLinks) {

        return new ResourceProcessor<Resource<SampleGroup>>() {

            @Override
            public Resource<SampleGroup> process(Resource<SampleGroup> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    addHistory(resource, repositoryEntityLinks);
                    addCurrentVersion(resource, repositoryEntityLinks);


                }

                return resource;
            }
        };
    }

    @Bean
    public ResourceProcessor<Resource<Study>> studyProcessor(RepositoryEntityLinks repositoryEntityLinks) {

        return new ResourceProcessor<Resource<Study>>() {

            @Override
            public Resource<Study> process(Resource<Study> resource) {

                if (resource.getContent().getDomain() != null &&
                        resource.getContent().getAlias() != null) {

                    addHistory(resource, repositoryEntityLinks);
                    addCurrentVersion(resource, repositoryEntityLinks);


                }

                return resource;
            }
        };
    }

    private void addHistory(Resource<? extends StoredSubmittable> resource, RepositoryEntityLinks repositoryEntityLinks) {
        StoredSubmittable item = resource.getContent();

        if (item.getDomain() != null && item.getDomain().getName() != null && item.getAlias() != null) {
            Map<String, String> expansionParams = new HashMap<>();

            expansionParams.put("domainName", item.getDomain().getName());
            expansionParams.put("alias", item.getAlias());

            Link contentsLink = repositoryEntityLinks.linkToSearchResource(item.getClass(), "history");

            Assert.notNull(contentsLink);


            resource.add(
                    contentsLink.expand(expansionParams)
            );

        }
    }

    private void addCurrentVersion(Resource<? extends StoredSubmittable> resource, RepositoryEntityLinks repositoryEntityLinks) {
        StoredSubmittable item = resource.getContent();

        if (item.getDomain() != null && item.getDomain().getName() != null && item.getAlias() != null) {
            Map<String, String> expansionParams = new HashMap<>();

            expansionParams.put("domainName", item.getDomain().getName());
            expansionParams.put("alias", item.getAlias());

            Link contentsLink = repositoryEntityLinks.linkToSearchResource(item.getClass(), "current-version");


            Assert.notNull(contentsLink);

            resource.add(
                    contentsLink.expand(expansionParams)
            );

        }
    }
}