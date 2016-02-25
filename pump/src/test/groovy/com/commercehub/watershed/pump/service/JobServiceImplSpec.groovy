package com.commercehub.watershed.pump.service

import com.commercehub.watershed.pump.application.factories.JobFactory
import com.commercehub.watershed.pump.application.factories.JobRunnableFactory
import com.commercehub.watershed.pump.model.Job
import com.commercehub.watershed.pump.model.JobPreview
import com.commercehub.watershed.pump.model.PreviewSettings
import com.commercehub.watershed.pump.model.PumpSettings
import com.commercehub.watershed.pump.processing.JobRunnable
import com.commercehub.watershed.pump.repositories.QueryableRepository
import spock.lang.Specification

import java.util.concurrent.ExecutorService

class JobServiceImplSpec extends Specification {

    JobService jobService
    Map<String, Job> jobMap
    JobRunnableFactory jobRunnableFactory
    ExecutorService executor
    QueryableRepository repository
    JobRunnable jobRunnable
    JobFactory jobFactory

    PumpSettings pumpSettings = new PumpSettings(queryIn: "select * from foo", streamOut: "MyStream")
    PreviewSettings previewSettings = new PreviewSettings(queryIn: "select * from foo", previewCount: 3)

    def setup() {
        jobMap = new HashMap<>()
        jobRunnableFactory = Mock(JobRunnableFactory)
        jobFactory = Mock(JobFactory)
        executor = Mock(ExecutorService)
        repository = Mock(QueryableRepository)
        jobRunnable = Mock(JobRunnable)

        jobService = new JobServiceImpl(
                jobMap: jobMap,
                jobRunnableFactory: jobRunnableFactory,
                jobFactory: jobFactory,
                executor: executor,
                repository: repository)
    }

    def "enqueueJob queues a job with the executor and adds it to the map"(){
        setup:
        jobFactory.create(_, _) >> new Job(null, UUID.randomUUID().toString(), pumpSettings)

        when:
        Job job = jobService.enqueueJob(pumpSettings)

        then:
        1 * jobRunnableFactory.create(_ as Job) >> jobRunnable
        1 * executor.submit(_ as JobRunnable)
        jobMap.size() == 1
        jobMap.get(job.jobId) == job
        job.pumpSettings == pumpSettings
    }

    def "getJob returns job from jobMap"(){
        setup:
        String jobId = UUID.randomUUID().toString()
        jobMap.put(jobId, new Job(null, jobId, pumpSettings))

        when:
        Job job = jobService.getJob(jobId)

        then:
        job == jobMap.get(jobId)
    }

    def "getJob returns null if not found in jobMap"(){
        setup:
        String jobId = UUID.randomUUID().toString()

        when:
        Job job = jobService.getJob(jobId)

        then:
        job == null
    }

    def "getAllJobs returns jobMap values"(){
        setup:
        String jobId = UUID.randomUUID().toString()
        jobMap.put(jobId, new Job(null, jobId, pumpSettings))

        when:
        Collection<Job> jobs = jobService.getAllJobs()

        then:
        jobs == jobMap.values()
    }

    def "getJobPreview returns jobPreview from repository"(){
        setup:
        JobPreview dummyJobPreview = new JobPreview(200, [["field1": "value1"], ["field2": "value2"], ["field2": "value2"]])

        when:
        JobPreview jobPreview = jobService.getJobPreview(previewSettings)

        then:
        1 * repository.getJobPreview(previewSettings) >> dummyJobPreview
        jobPreview == dummyJobPreview
    }
}
