package uk.ac.ebi.subs.api.services;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.subs.repository.model.Sample;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
public class SubmittableValidationDispatcherUtilTests {

    private SubmittableValidationDispatcher submittableValidationDispatcher;

    @Before
    public void buildUp(){
        //we're not going to test sending messages in this class
        submittableValidationDispatcher = new SubmittableValidationDispatcher(null);
    }

    @Test
    public void givenSample_ensureBaseSubmittableShouldNotThrowAnException() {
        submittableValidationDispatcher.ensureBaseSubmittable(new Sample());
    }

    @Test
    public void givenSample_submittableQueueSuffixShouldBeLowerCaseShortClassName(){
        String actual = submittableValidationDispatcher.submittableQueueSuffix(new Sample());
        assertThat(actual,equalTo("sample"));
    }

}
