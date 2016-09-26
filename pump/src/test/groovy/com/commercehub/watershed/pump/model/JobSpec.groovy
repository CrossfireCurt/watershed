package com.commercehub.watershed.pump.model

import com.commercehub.watershed.pump.service.TimeService
import org.joda.time.Instant
import spock.lang.Specification

class JobSpec extends Specification{

    Job job
    TimeService timeService
    String jobId = UUID.randomUUID().toString()
    PumpSettings pumpSettings = new PumpSettings(queryIn: "select * from foo", streamOut: "MyStream")

    def setup(){
        timeService = Mock(TimeService)
        job = new Job(timeService, jobId, pumpSettings)
    }

    def "getElapsedTime: elapsedTime is 0 if both start and completion times don't exist"(){
        setup:
        job.setCompletionTime(null)
        job.setStartTime(null)

        when:
        Long elapsedTime = job.getElapsedTime()

        then:
        elapsedTime == 0L
        job.getElapsedTimePretty() == "0.000 seconds"
    }

    def "getElapsedTime: completionTime is null, startTime is not, use current system time"(){
        setup:
        timeService.currentTimeMillis() >> Instant.parse("2015-11-23T15:12:55.100Z").toDateTime().getMillis()

        job.setCompletionTime(null)
        job.setStartTime(Instant.parse("2015-11-23T15:12:55.000Z").toDateTime())

        when:
        Long elapsedTime = job.getElapsedTime()

        then:
        elapsedTime == 100L
    }

    def "getElapsedTime: completionTime is set, startTime is set"(){
        setup:
        job.setStartTime(Instant.parse("2015-11-23T15:12:55.000Z").toDateTime())
        job.setCompletionTime(Instant.parse("2015-11-23T15:12:55.500Z").toDateTime())

        when:
        Long elapsedTime = job.getElapsedTime()

        then:
        elapsedTime == 500L
        job.getElapsedTimePretty() == "0.500 seconds"
    }

    def "getElapsedTime: completionTime is set, startTime is set, expecting minutes"(){
        setup:
        job.setStartTime(Instant.parse("2015-11-23T15:12:55.000Z").toDateTime())
        job.setCompletionTime(Instant.parse("2015-11-23T15:14:56.500Z").toDateTime())

        when:
        Long elapsedTime = job.getElapsedTime()

        then:
        elapsedTime == 121500L
        job.getElapsedTimePretty() == "2 minutes, 1.500 seconds"
    }

    def "getElapsedTime: completionTime is set, startTime is set, expecting hours"(){
        setup:
        job.setStartTime(Instant.parse("2015-11-23T15:12:55.000Z").toDateTime())
        job.setCompletionTime(Instant.parse("2015-11-23T16:14:56.500Z").toDateTime())

        when:
        Long elapsedTime = job.getElapsedTime()

        then:
        elapsedTime == 3721500L
        job.getElapsedTimePretty() == "1 hour, 2 minutes, 1.500 seconds"
    }

    def "getMeanRate: elapsed time is 0"(){
        setup:
        job.setCompletionTime(null)
        job.setStartTime(null)

        when:
        Long meanRate = job.getMeanRate()

        then:
        meanRate == null
        job.getMeanRatePretty() == "--- rec/s"
    }

    def "getMeanRate: elapsed time is not 0, successfulRecordCount + failureRecordCount is 0"(){
        setup:
        job.setStartTime(Instant.parse("2015-11-23T15:12:55.000Z").toDateTime())
        job.setCompletionTime(Instant.parse("2015-11-23T15:12:55.500Z").toDateTime())
        job.setSuccessfulRecordCount(0)
        job.setFailureRecordCount(0)

        when:
        Long meanRate = job.getMeanRate()

        then:
        meanRate == 0L
        job.getMeanRatePretty() == "0.0 rec/s"
    }

    def "getMeanRate: elapsed time is not 0, successfulRecordCount + failureRecordCount is not 0"(){
        setup:
        job.setStartTime(Instant.parse("2015-11-23T15:12:55.000Z").toDateTime())
        job.setCompletionTime(Instant.parse("2015-11-23T15:12:55.500Z").toDateTime())
        job.setSuccessfulRecordCount(1)
        job.setFailureRecordCount(1)

        when:
        Long meanRate = job.getMeanRate()

        then:
        meanRate == 4L
        job.getMeanRatePretty() == "4.0 rec/s"
    }
}
