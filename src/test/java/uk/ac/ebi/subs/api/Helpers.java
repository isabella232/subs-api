package uk.ac.ebi.subs.api;

import uk.ac.ebi.subs.api.controllers.SubmissionDTO;
import uk.ac.ebi.subs.data.client.Study;
import uk.ac.ebi.subs.data.component.AssayRef;
import uk.ac.ebi.subs.data.component.Attribute;
import uk.ac.ebi.subs.data.component.File;
import uk.ac.ebi.subs.data.component.ProjectRef;
import uk.ac.ebi.subs.data.component.SampleRef;
import uk.ac.ebi.subs.data.component.SampleUse;
import uk.ac.ebi.subs.data.component.StudyRef;
import uk.ac.ebi.subs.data.component.Submitter;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.data.component.Term;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionPlan;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.validator.data.SingleValidationResult;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.data.structures.GlobalValidationStatus;
import uk.ac.ebi.subs.validator.data.structures.SingleValidationResultStatus;
import uk.ac.ebi.subs.validator.data.structures.ValidationAuthor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Helpers {

    public final static String TEAM_NAME = "subs.dev-team-1";
    public final static String ADMIN_TEAM_NAME = "self.embl-ebi-subs-admin";

    public static final String FILE_UPLOAD_PATH = "/file/upload/path";
    public static final String FILE_TARGET_PATH = "/file/target/path";
    public static final String FILE_CREATED_BY = "test_user";
    public static final String FILE_CHECKSUM = "12345678901234567890abcdef0123ab";
    private static final String TEST_SUBMISSION_PLAN_URI = "submissionPlans";

    public static Submission generateSubmission() {
        Submission s = new Submission();

        s.setSubmitter(generateTestSubmitter());
        s.setSubmissionPlan(generateSubmissionPlan());

        return s;
    }

    public static SubmissionDTO generateSubmissionDTO(String rootURI, SubmissionPlan submissionPlan) {
        SubmissionDTO submissionDTO = new SubmissionDTO();

        submissionDTO.setSubmissionPlan(getSubmissionPlanURI(rootURI, submissionPlan));

        return submissionDTO;
    }

    private static Submitter generateTestSubmitter() {
        Submitter u = new Submitter();
        u.setEmail("test@test.org");
        return u;
    }

    public static List<uk.ac.ebi.subs.data.client.Sample> generateTestClientSamples(int numberOfSamplesRequired) {
        List<uk.ac.ebi.subs.data.client.Sample> samples = new ArrayList<>(numberOfSamplesRequired);

        for (int i = 1; i <= numberOfSamplesRequired; i++) {
            uk.ac.ebi.subs.data.client.Sample s = new uk.ac.ebi.subs.data.client.Sample();
            samples.add(s);

            s.setAlias("D" + i);
            s.setTitle("NA12878_D" + i);
            s.setDescription("Material derived from cell line NA12878");
            s.setTaxon("Homo sapiens");
            s.setTaxonId(9606L);
            s.setReleaseDate(LocalDate.of(2017, Month.JANUARY, 1));

            Attribute cellLineType = attribute("EBV-LCL cell line");
            Term ebvLclCellLine = new Term();
            ebvLclCellLine.setUrl("http://purl.obolibrary.org/obo/BTO_0003335");
            cellLineType.getTerms().add(ebvLclCellLine);

            s.getAttributes().put("Cell line type", Collections.singletonList(cellLineType));
        }

        return samples;
    }

    public static List<uk.ac.ebi.subs.data.client.Study> generateTestClientStudies(int numberOfStudiesRequired) {
        return generateTestClientStudies(numberOfStudiesRequired, generateClientProject().getAlias());
    }

    public static List<uk.ac.ebi.subs.data.client.Study> generateTestClientStudies(int numberOfStudiesRequired, String projectAlias) {
        List<uk.ac.ebi.subs.data.client.Study> studies = new ArrayList<>(numberOfStudiesRequired);

        for (int i = 1; i <= numberOfStudiesRequired; i++) {
            uk.ac.ebi.subs.data.client.Study s = new uk.ac.ebi.subs.data.client.Study();
            studies.add(s);

            Attribute studyType = new Attribute();
            studyType.setValue("Whole Genome Sequencing");

            s.setAlias("Study" + i);
            s.setTitle("My Sequencing Study " + i);
            s.setDescription("We sequenced some humans to discover variants linked with a disease");

            ProjectRef projectRef = new ProjectRef();
            projectRef.setAlias(projectAlias);
            s.setProjectRef(projectRef);

            Attribute studyAbstract = new Attribute();
            studyAbstract.setValue(s.getDescription());

            s.getAttributes().put("study_type", Collections.singletonList(studyType));
            s.getAttributes().put("study_abstract", Collections.singletonList(studyAbstract));
        }

        return studies;
    }

    public static Attribute attribute(String value) {
        Attribute attribute = new Attribute();
        attribute.setValue(value);
        return attribute;
    }

    public static uk.ac.ebi.subs.data.client.Project generateClientProject() {
        uk.ac.ebi.subs.data.client.Project project = new uk.ac.ebi.subs.data.client.Project();
        project.setAlias("example-short-unique-name");
        project.setTitle("Example title for our scientific project, at least 50 characters long");
        project.setDescription("Example description for our scientific project, which must also be at least 50 characters long");
        project.setReleaseDate(LocalDate.now());
        return project;
    }

    public static List<uk.ac.ebi.subs.data.client.Assay> generateTestClientAssays(int numberOfAssaysRequired) {
        List<uk.ac.ebi.subs.data.client.Assay> assays = new ArrayList<>(numberOfAssaysRequired);

        Study study = generateTestClientStudies(1).get(0);
        StudyRef studyRef = new StudyRef();
        studyRef.setAlias(study.getAlias());

        List<uk.ac.ebi.subs.data.client.Sample> samples = generateTestClientSamples(numberOfAssaysRequired);

        for (int i = 1; i <= numberOfAssaysRequired; i++) {
            uk.ac.ebi.subs.data.client.Assay a = new uk.ac.ebi.subs.data.client.Assay();
            assays.add(a);

            a.setAlias("A" + i);
            a.setTitle("Assay " + i);
            a.setDescription("Human sequencing experiment");

            a.setStudyRef(studyRef);

            SampleRef sampleRef = new SampleRef();
            sampleRef.setAlias(samples.get(i - 1).getAlias());

            SampleUse sampleUse = new SampleUse();
            sampleUse.setSampleRef(sampleRef);
            a.getSampleUses().add(sampleUse);

            a.getAttributes().put("library_strategy", Collections.singletonList(attribute("WGS")));
            a.getAttributes().put("library_source", Collections.singletonList(attribute("GENOMIC")));
            a.getAttributes().put("library_selection", Collections.singletonList(attribute("RANDOM")));
            a.getAttributes().put("library_layout", Collections.singletonList(attribute("SINGLE")));

            a.getAttributes().put("platform_type", Collections.singletonList(attribute("ILLUMINA")));
            a.getAttributes().put("instrument_model", Collections.singletonList(attribute("Illumina HiSeq 2000")));
        }

        return assays;
    }

    public static List<uk.ac.ebi.subs.data.client.AssayData> generateTestClientAssayData(int numberOfAssaysRequired) {
        List<uk.ac.ebi.subs.data.client.AssayData> assayData = new ArrayList<>(numberOfAssaysRequired);


        List<uk.ac.ebi.subs.data.client.Assay> assays = generateTestClientAssays(numberOfAssaysRequired);


        for (int i = 1; i <= numberOfAssaysRequired; i++) {
            uk.ac.ebi.subs.data.client.AssayData ad = new uk.ac.ebi.subs.data.client.AssayData();
            assayData.add(ad);


            ad.setAlias("AD" + i);
            ad.setTitle("AssayData" + i);
            ad.setDescription("Human sequencing experiment run");

            AssayRef assayRef = new AssayRef();
            assayRef.setAlias(assays.get(i - 1).getAlias());
            ad.setAssayRefs(Arrays.asList(assayRef));

            File file = new File();
            file.setName("sequencingData.cram");
            file.setType("cram");
            file.setChecksum("4bb1c4561d99d88c8b38a40d694267dc");
            ad.getFiles().add(file);

            ad.getAttributes().put("assay_data_attribute", Collections.singletonList(attribute("attribute value")));
        }

        return assayData;
    }

    public static List<Sample> generateTestSamples(int numberOfSamplesRequired) {
        return generateTestSamples(numberOfSamplesRequired, true);
    }

    public static List<Sample> generateTestSamples(int numberOfSamplesRequired, boolean createProcessingStatus) {
        List<Sample> samples = new ArrayList<>(numberOfSamplesRequired);

        for (int i = 1; i <= numberOfSamplesRequired; i++) {
            Sample s = new Sample();
            samples.add(s);

            s.setId(createId());
            s.setTeam(generateTestTeam());
            s.setAlias("D" + i);
            s.setTitle("Donor " + i);
            s.setDescription("Human sample donor");
            s.setTaxon("Homo sapiens");
            s.setTaxonId(9606L);

            if (createProcessingStatus) {
                s.setProcessingStatus(new ProcessingStatus(ProcessingStatusEnum.Draft));
            }
        }

        return samples;
    }

    public static List<Project> generateTestProjects(int numberRequired) {
        List<Project> projects = new ArrayList<>(numberRequired);

        for (int i = 1; i <= numberRequired; i++) {
            Project p = new Project();
            projects.add(p);

            p.setId(createId());
            p.setTeam(generateTestTeam());
            p.setAlias("P" + i);
            p.setTitle("Project" + i);
            p.setDescription("A great project");

            p.setProcessingStatus(new ProcessingStatus(ProcessingStatusEnum.Draft));
        }

        return projects;
    }

    public static Team generateTestTeam() {
        Team d = new Team();
        d.setName(TEAM_NAME);
        return d;
    }

    public static Submission generateTestSubmission() {
        Submission sub = new Submission();
        Team d = generateTestTeam();
        sub.setId(createId());

        sub.setTeam(d);

        sub.setSubmissionStatus(new SubmissionStatus(SubmissionStatusEnum.Draft));
        sub.getSubmissionStatus().setTeam(d);
        sub.setSubmissionPlan(generateSubmissionPlan());
        return sub;
    }

    private static String createId() {
        return UUID.randomUUID().toString();
    }

    public static String getRandomAlias() {
        Random random = new Random();
        return String.format("%04d", random.nextInt(10000));
    }

    public static uk.ac.ebi.subs.repository.model.fileupload.File generateFileWithFileName(
            String filename, String submissionID) {
        uk.ac.ebi.subs.repository.model.fileupload.File file = new uk.ac.ebi.subs.repository.model.fileupload.File();
        file.setId(UUID.randomUUID().toString());
        file.setFilename(filename);
        file.setSubmissionId(submissionID);
        file.setGeneratedTusId(UUID.randomUUID().toString().replace("-", ""));
        file.setUploadPath(FILE_UPLOAD_PATH);
        file.setTargetPath(FILE_TARGET_PATH);
        file.setCreatedBy(FILE_CREATED_BY);
        file.setUploadStartDate(LocalDateTime.now().minusSeconds(90));
        file.setUploadFinishDate(LocalDateTime.now());
        file.setStatus(FileStatus.READY_FOR_ARCHIVE);
        file.setChecksum(FILE_CHECKSUM);

        return file;

    }

    public static ValidationResult generateTestValidationResult(String submissionID) {
        SingleValidationResult singleValidationResult = new SingleValidationResult();
        singleValidationResult.setValidationStatus(SingleValidationResultStatus.Pass);

        ValidationResult validationResult = new ValidationResult();
        validationResult.setSubmissionId(submissionID);
        validationResult.setUuid("test");
        validationResult.getExpectedResults().put(ValidationAuthor.Core, Collections.singletonList(singleValidationResult));
        validationResult.setValidationStatus(GlobalValidationStatus.Complete);

        return validationResult;
    }

    public static SubmissionPlan generateSubmissionPlan() {
        SubmissionPlan submissionPlan = new SubmissionPlan();
        submissionPlan.setId(UUID.randomUUID().toString());
        submissionPlan.setDescription("Test submission Plan description");
        submissionPlan.setDisplayName("Test display name for submission plan");
        submissionPlan.setDataTypeIds(Arrays.asList("samples", "sequencingExperiments", "enaStudies"));

        return submissionPlan;
    }

    public static String getSubmissionPlanURI(String rootURI, SubmissionPlan submissionPlan) {
        return String.join("/", rootURI, TEST_SUBMISSION_PLAN_URI, submissionPlan.getId()) ;
    }
}
