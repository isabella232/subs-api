package uk.ac.ebi.subs.repository.repos.submittables.support;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import uk.ac.ebi.subs.data.component.AbstractSubsRef;
import uk.ac.ebi.subs.data.submittable.BaseSubmittable;
import uk.ac.ebi.subs.data.submittable.Submittable;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.replaceRoot;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.skip;
import static org.springframework.data.mongodb.core.query.Criteria.where;

public class SubmittablesAggregateSupport<T extends StoredSubmittable> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private MongoTemplate mongoTemplate;
    private Class<T> clazz;
    private ValidationResultRepository validationResultRepository;

    public SubmittablesAggregateSupport(MongoTemplate mongoTemplate,
                                        ValidationResultRepository validationResultRepository, Class<T> clazz) {
        this.mongoTemplate = mongoTemplate;
        this.validationResultRepository = validationResultRepository;
        this.clazz = clazz;
    }

    /**
     * find instances of T that are in the submission and have a reference matching ref
     *
     * @param submissionId
     * @param ref
     * @return
     */
    public List<T> findBySubmissionIdAndReference(String submissionId, AbstractSubsRef ref){

        boolean haveAccession = ref.getAccession() != null;
        boolean haveAlias = ref.getAlias() != null;

        Criteria aliasMatch = Criteria.where("alias").is(ref.getAlias()).and("team").is(ref.getTeam());
        Criteria accessionMatch = Criteria.where("accession").is(ref.getAccession());

        Criteria elemMatchCriteria;


        if (haveAccession && haveAlias){
            elemMatchCriteria = new Criteria().orOperator(aliasMatch, accessionMatch);
        }
        else if (haveAccession){
            elemMatchCriteria = accessionMatch;
        }
        else if (haveAlias){
            elemMatchCriteria = aliasMatch;
        }
        else {
            elemMatchCriteria = new Criteria();
        }


        Query query = new Query();
        query.addCriteria(
                Criteria.where("submission.$id").is(submissionId)
                        .and("references."+ref.getClass().getSimpleName()).elemMatch(elemMatchCriteria)
        );


        return mongoTemplate.find(query,clazz);
    }

    public Page<T> itemsByTeam(String teamName, Pageable pageable) {
        return itemsByTeams(Arrays.asList(teamName), pageable);
    }

    public Page<T> itemsByTeams(List<String> teamNames, Pageable pageable) {
        List<T> resultsList = getLimitedItemListByTeams(teamNames, pageable);
        long totalItemsCount = getTotalItemCountByTeams(teamNames);
        return new PageImpl<T>(resultsList, pageable, totalItemsCount);
    }

    public Page<T> submittablesByDataTypeWithErrors(String submissionId, String dataTypeId, Pageable pageable) {
        Page<ValidationResult> pageOfValidationResults =
                validationResultRepository.findBySubmissionIdAndDataTypeIdAndHasError(
                        submissionId, dataTypeId, true, pageable);
        return getPageOfSubmittablesByUuids(pageable, pageOfValidationResults);
    }

    public Page<T> submittablesByDataTypeWithWarnings(String submissionId, String dataTypeId, Pageable pageable) {
        Page<ValidationResult> pageOfValidationResults =
                validationResultRepository.findBySubmissionIdAndDataTypeIdAndHasWarning(
                        submissionId, dataTypeId, true, pageable);
        return getPageOfSubmittablesByUuids(pageable, pageOfValidationResults);
    }

    private Page<T> getPageOfSubmittablesByUuids(Pageable pageable, Page<ValidationResult> pageOfValidationResults) {
        List<String> entityUuids = pageOfValidationResults.map(ValidationResult::getEntityUuid).getContent();
        long totalItemsCount = pageOfValidationResults.getTotalElements();

        List<T> submittablesWithWarnings = getListOfsubmittablesByEntityUuids(entityUuids, pageable);
        return new PageImpl<T>(submittablesWithWarnings, pageable, totalItemsCount);
    }

    private long getTotalItemCountByTeams(List<String> teamNames) {
        AggregationResults aggregationResults = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        teamMatchOperation(teamNames),
                        groupByAlias(),
                        group().count().as("count")
                ),
                clazz, clazz
        );

        Object results = aggregationResults.getRawResults().get("result");

        if (results != null && results instanceof BasicDBList) {
            BasicDBList resultsList = (BasicDBList) results;

            if (resultsList.isEmpty()) {
                return 0;
            }
            BasicDBObject countResult = (BasicDBObject) resultsList.get(0);

            if (countResult.containsField("count")) {
                return countResult.getLong("count");
            }

            return 0;


        }

        return -1;
    }

    private List<T> getLimitedItemListByTeams(List<String> teamNames, Pageable pageable) {
        final List<T> resultsList = new ArrayList<>();

        final List<AggregationOperation> aggOps = new ArrayList<>(Arrays.asList(
                teamMatchOperation(teamNames),
                sortAliasCreatedDate(),
                groupByAliasWithFirstItem(),
                skip((long) pageable.getOffset()),
                limit((long) pageable.getPageSize()),
                replaceRoot("first")
        ));

        if (pageable.getSort().isSorted()) {
            aggOps.add(new SortOperation(pageable.getSort()));
        }

        AggregationResults aggregationResults = mongoTemplate.aggregate(Aggregation.newAggregation(
                aggOps), clazz, clazz);

        return aggregationResults.getMappedResults();
    }

    private List<T> getListOfsubmittablesByEntityUuids(List<String> entityUuids, Pageable pageable) {
        final List<AggregationOperation> aggOps = new ArrayList<>(Arrays.asList(
                entityUuidMatchOperation(entityUuids)
        ));

        if (pageable.getSort().isSorted()) {
            aggOps.add(new SortOperation(pageable.getSort()));
        }

        AggregationResults aggregationResults = mongoTemplate.aggregate(Aggregation.newAggregation(
                aggOps), clazz, clazz);

        return aggregationResults.getMappedResults();
    }

    private GroupOperation groupByAliasWithFirstItem() {
        return group("alias", "team.name").first("$$ROOT").as("first");
    }

    private GroupOperation groupByAlias() {
        return group("alias", "team.name");
    }

    private SortOperation sortAliasCreatedDate() {
        return Aggregation.sort(Sort.Direction.DESC, "alias").and(Sort.Direction.DESC, "team.name").and(Sort.Direction.DESC, "createdDate");
    }

    private MatchOperation teamMatchOperation(List<String> teamNames) {
        return match(where("team.name").in(teamNames));
    }

    private MatchOperation entityUuidMatchOperation(List<String> entityUuids) {
        return match(where("_id").in(entityUuids));
    }
}
