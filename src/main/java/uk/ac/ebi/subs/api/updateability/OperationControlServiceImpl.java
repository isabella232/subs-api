package uk.ac.ebi.subs.api.updateability;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.status.ProcessingStatus;
import uk.ac.ebi.subs.repository.SubmissionRepository;

@Service
public class OperationControlServiceImpl implements OperationControlService {

    private static final Logger logger = LoggerFactory.getLogger(OperationControlService.class);

    @Autowired
    SubmissionRepository submissionRepository;


    @Override
    public boolean isUpdateable(Submission submission) {
        if (submission.getStatus() != null && submission.getStatus().equals(ProcessingStatus.Draft.name())){
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public boolean isSubmissionUpdateable(String submissionId) {
        Submission submission = submissionRepository.findOne(submissionId);

        if (submission == null){
            throw new ResourceNotFoundException();
        }

        return this.isUpdateable(submission);
    }
}
