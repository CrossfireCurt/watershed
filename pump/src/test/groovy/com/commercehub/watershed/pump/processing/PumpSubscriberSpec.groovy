package com.commercehub.watershed.pump.processing

import com.amazonaws.services.kinesis.producer.UserRecordResult
import com.commercehub.watershed.pump.model.Job
import com.commercehub.watershed.pump.model.ProcessingStage
import com.commercehub.watershed.pump.model.PumpRecordResult
import com.commercehub.watershed.pump.model.PumpSettings
import com.commercehub.watershed.pump.model.DrillResultRow
import rx.Producer
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicLong

public class PumpSubscriberSpec extends Specification {
    PumpSubscriber pumpSubscriber
    Job job
    Pump pump
    Producer producer
    UserRecordResult userRecordResult

    def setup(){
        job = new Job(null, "test", new PumpSettings())
        pump = Mock(Pump)
        producer = Mock(Producer)
        pumpSubscriber = new PumpSubscriber(job, pump, 5)
        pumpSubscriber.setProducer(producer)

        userRecordResult = Mock(UserRecordResult)
    }

    def "onStart sets up job"(){
        when:
        pumpSubscriber.onStart()

        then:
        job.startTime != null
        job.stage == ProcessingStage.IN_PROGRESS
        job.failureRecordCount == 0L
        job.successfulRecordCount == 0L
    }

    def "onStart kicks off requests"(){
        when:
        pumpSubscriber.onStart()

        then:
        1 * producer.request(5)
    }

    def "onStart still kick off requests with null job"(){
        when:
        pumpSubscriber.job = null
        pumpSubscriber.onStart()

        then:
        notThrown(NullPointerException)

        then:
        1 * producer.request(5)
    }

    def "onCompleted shuts down pump"(){
        when:
        pumpSubscriber.onCompleted()

        then:
        1 * pump.flushSync()

        then:
        1 * pump.destroy()
    }

    def "onCompleted doesn't shut down pump if its null"(){
        when:
        pumpSubscriber.pump = null
        pumpSubscriber.onCompleted()

        then:
        notThrown(NullPointerException)

        then:
        0 * pump.flushSync()
        0 * pump.destroy()
    }

    def "onCompleted wraps up job"(){
        when:
        pumpSubscriber.onCompleted()

        then:
        job.stage == ProcessingStage.COMPLETED_SUCCESS
        job.completionTime != null
    }

    def "onCompleted doesn't wrap up job if its null"(){
        when:
        pumpSubscriber.job = null
        pumpSubscriber.onCompleted()

        then:
        notThrown(NullPointerException)

        then:
        job.stage != ProcessingStage.COMPLETED_SUCCESS
        job.completionTime == null
    }

    def "updateStats updates job"(){
        when:
        pumpSubscriber.failCount = new AtomicLong(1L)
        pumpSubscriber.successCount = new AtomicLong(5L)
        pumpSubscriber.updateStats()

        then:
        2 * pump.countPending() >> 10
        job.failureRecordCount == 1L
        job.successfulRecordCount == 5L
        job.pendingRecordCount == 10L
    }

    def "updateStats doesn't update job if job is null"(){
        when:
        pumpSubscriber.job = null
        pumpSubscriber.updateStats()

        then:
        notThrown(NullPointerException)

        then:
        1 * pump.countPending()
        job.failureRecordCount == null
        job.successfulRecordCount == null
        job.pendingRecordCount == null
    }

    def "updateStats doesn't update pending stat if pump is null"(){
        when:
        pumpSubscriber.pump = null
        pumpSubscriber.failCount = new AtomicLong(1L)
        pumpSubscriber.successCount = new AtomicLong(5L)
        pumpSubscriber.updateStats()

        then:
        notThrown(NullPointerException)

        then:
        0 * pump.countPending()
        job.failureRecordCount == 1L
        job.successfulRecordCount == 5L
        job.pendingRecordCount == null
    }

    def "onError destroys pump and wraps up job"(){
        when:
        Exception ex = new Exception("error")
        pumpSubscriber.onError(ex)

        then:
        1 * pump.destroy()
        job.completionTime != null
        job.stage == ProcessingStage.COMPLETED_ERROR
        job.processingErrors.size() == 1
        job.processingErrors.get(0) == ex
    }

    def "onError still wraps up job if pump is null"(){
        when:
        pumpSubscriber.pump = null
        Exception ex = new Exception("error")
        pumpSubscriber.onError(ex)

        then:
        notThrown(NullPointerException)

        then:
        0 * pump.destroy()
        job.completionTime != null
        job.stage == ProcessingStage.COMPLETED_ERROR
        job.processingErrors.size() == 1
        job.processingErrors.get(0) == ex
    }

    def "onError still destroys pump if job is null"(){
        when:
        pumpSubscriber.job = null
        Exception ex = new Exception("error")
        pumpSubscriber.onError(ex)

        then:
        notThrown(NullPointerException)

        then:
        1 * pump.destroy()
        job.completionTime == null
        job.processingErrors.size() == 0
    }

    def "onNext increments success count"(){
        setup:
        userRecordResult.isSuccessful() >> true

        when:
        pumpSubscriber.onNext(new PumpRecordResult(userRecordResult, Mock(DrillResultRow)))

        then:
        pumpSubscriber.successCount.get() == 1
    }

    def "onNext increments failure count"(){
        setup:
        userRecordResult.isSuccessful() >> false

        when:
        pumpSubscriber.onNext(new PumpRecordResult(userRecordResult, Mock(DrillResultRow)))

        then:
        pumpSubscriber.failCount.get() == 1
    }

    def "onNext updatesStats if first record"(){
        setup:
        userRecordResult.isSuccessful() >> true

        when:
        pumpSubscriber.onNext(new PumpRecordResult(userRecordResult, Mock(DrillResultRow)))

        then:
        pumpSubscriber.successCount.get() == 1
        job.successfulRecordCount == pumpSubscriber.successCount.get()
    }

    def "onNext doesn't updateStats in the middle of chunk processing"(){
        setup:
        userRecordResult.isSuccessful() >> true

        when:
        pumpSubscriber.successCount = new AtomicLong(3L)
        pumpSubscriber.onNext(new PumpRecordResult(userRecordResult, Mock(DrillResultRow)))

        then:
        job.successfulRecordCount != pumpSubscriber.successCount.get()
    }

    def "onNext updateStats for start of chunk processing"(){
        setup:
        userRecordResult.isSuccessful() >> true

        when:
        pumpSubscriber.successCount = new AtomicLong(4L)
        pumpSubscriber.onNext(new PumpRecordResult(userRecordResult, Mock(DrillResultRow)))

        then:
        job.successfulRecordCount == pumpSubscriber.successCount.get()
    }

    def "onNext doesn't do a new request chunk for middle of chunk processing"(){
        when:
        pumpSubscriber.successCount = new AtomicLong(3L)
        pumpSubscriber.onNext(new PumpRecordResult(userRecordResult, Mock(DrillResultRow)))

        then:
        0 * producer.request(_)
    }

    def "onNext requests new chunk at end of chunk processing"(){
        when:
        pumpSubscriber.successCount = new AtomicLong(4L)
        pumpSubscriber.onNext(new PumpRecordResult(userRecordResult, Mock(DrillResultRow)))

        then:
        1 * producer.request(5)
    }
}
