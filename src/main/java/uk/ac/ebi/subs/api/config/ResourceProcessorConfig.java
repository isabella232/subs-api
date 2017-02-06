package uk.ac.ebi.subs.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.data.util.Pair;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.SubmissionLinks;
import uk.ac.ebi.subs.repository.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Configuration
public class ResourceProcessorConfig {

    @Autowired
    RepositoryEntityLinks repositoryEntityLinks;

    @Bean
    ResourceProcessor<Resource<Submission>> submissionProcessor() {
        return new ResourceProcessor<Resource<Submission>>() {
            @Override
            public Resource<Submission> process(Resource<Submission> resource) {
                Submission submission = resource.getContent();

                Map<String, String> linkArgs = new HashMap<>();
                linkArgs.put("submissionId", submission.getId());

                Stream<Pair<String, Class>> relsToTypes = Stream.of(
                        Pair.of(SubmissionLinks.ANALYSIS, Analysis.class),
                        Pair.of(SubmissionLinks.ASSAY_DATA, AssayData.class),
                        Pair.of(SubmissionLinks.ASSAY, Assay.class),
                        Pair.of(SubmissionLinks.EGA_DAC_POLICY, EgaDacPolicy.class),
                        Pair.of(SubmissionLinks.EGA_DAC, EgaDac.class),
                        Pair.of(SubmissionLinks.EGA_DATASET, EgaDataset.class),
                        Pair.of(SubmissionLinks.PROJECT, Project.class),
                        Pair.of(SubmissionLinks.PROTOCOL, Protocol.class),
                        Pair.of(SubmissionLinks.SAMPLE_GROUP, SampleGroup.class),
                        Pair.of(SubmissionLinks.SAMPLE, Sample.class),
                        Pair.of(SubmissionLinks.STUDY, Study.class)
                );

                relsToTypes.forEach(pair -> {
                    String rel = pair.getFirst();
                    Class domainType = pair.getSecond();

                    resource.add(
                            repositoryEntityLinks.linkToSearchResource(domainType, rel).expand(linkArgs)
                    );
                });

                return resource;
            }
        };
    }


}
