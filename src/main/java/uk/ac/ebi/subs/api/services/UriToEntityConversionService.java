package uk.ac.ebi.subs.api.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UriToEntityConversionService {

    @NonNull
    private MappingContext<?, ?> mappingContext; // OOTB

    @NonNull
    private RepositoryInvokerFactory invokerFactory; // OOTB

    @NonNull
    private Repositories repositories; // OOTB

    public <T> T convert(URI uri, Class<T> target) {

        PersistentEntities entities = new PersistentEntities(Collections.singletonList(mappingContext));
        UriToEntityConverter converter = new UriToEntityConverter(entities, invokerFactory, repositories);

        Object o = converter.convert(uri, TypeDescriptor.valueOf(URI.class), TypeDescriptor.valueOf(target));
        T object = target.cast(o);
        if (object == null) {
            throw new IllegalArgumentException(String.format("%s '%s' was not found.", target.getSimpleName(), uri));
        }
        return object;
    }
}
