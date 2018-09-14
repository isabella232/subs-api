package uk.ac.ebi.subs.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.RelProvider;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.repos.submittables.SubmittableRepository;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SubmittablesConfig {

    @Bean
    public Map<String, Class<? extends StoredSubmittable>> submittablesByCollectionName(Map<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> submittableRepositoryMap, RelProvider relProvider){
        Map<String, Class<? extends StoredSubmittable>> classesByCollectionName = new HashMap<>();

        for (Map.Entry<Class<? extends StoredSubmittable>, SubmittableRepository<? extends StoredSubmittable>> entry : submittableRepositoryMap.entrySet()) {
            Class<? extends StoredSubmittable> submittableClass = entry.getKey();

            String collectionName = relProvider.getCollectionResourceRelFor(submittableClass);

            classesByCollectionName.put(collectionName, submittableClass);
        }

        return classesByCollectionName;
    }




}
