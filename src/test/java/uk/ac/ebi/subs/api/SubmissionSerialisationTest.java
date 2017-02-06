package uk.ac.ebi.subs.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.data.FullSubmission;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.repository.model.Sample;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@JsonTest
public class SubmissionSerialisationTest {

    @Autowired
    private JacksonTester<FullSubmission> json;

    @Test
    public void testSerialize() throws Exception {
        assertThat(this.json.write(sub)).isEqualToJson(exampleJson);
    }

    String exampleJson;
    FullSubmission sub;

    @Before
    public void setUp() throws IOException {
        sub = new FullSubmission();
        sub.getDomain().setName("exampleDomain");
        sub.getSubmitter().setEmail("test@example.ac.uk");
        sub.getSamples().add(new Sample());

        exampleJson = "{\"submitter\":{\"email\":\"test@example.ac.uk\"},\"domain\":{\"name\":\"exampleDomain\"}, \"samples\": [{}]}";
    }

    @Test
    public void testDeserialize() throws Exception {

        ObjectContent<FullSubmission> deserializedSub = this.json.parse(exampleJson);
        Submission actualSub = sub;

        assertThat(actualSub.getDomain())
                .isEqualTo(sub.getDomain());
        assertThat(actualSub.getSubmitter())
                .isEqualTo(sub.getSubmitter());
        assertThat(actualSub.getSubmissionDate()).isEqualTo(sub.getSubmissionDate());
    }


}
