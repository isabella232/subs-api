package uk.ac.ebi.subs.api.aap;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamNameSequenceService {

    @NonNull
    private MongoTemplate mongoTemplate;

    @Value("${usi.teamName.prefix}")
    private String domainPrefix;


    private String sequenceName(){
        return domainPrefix;
    }

    private TeamNameSequence nextTeamNameSequence(){
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is( sequenceName()));

        Update update = new Update();
        update.inc("sequenceValue", 1);

        FindAndModifyOptions options = new FindAndModifyOptions();
        options.upsert(true);
        options.returnNew(true);

        TeamNameSequence teamNameSequence = mongoTemplate.findAndModify(query,update,options,TeamNameSequence.class);

        return teamNameSequence;
    }
    public String nextTeamName(){
        TeamNameSequence teamNameSequence = nextTeamNameSequence();
        return teamNameSequence.getTeamNamePrefix()+teamNameSequence.getSequenceValue();
    }


}
