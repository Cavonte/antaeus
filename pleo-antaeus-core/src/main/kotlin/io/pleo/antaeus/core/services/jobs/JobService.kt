package io.pleo.antaeus.core.services.jobs

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import org.jeasy.batch.core.job.Job
import org.jeasy.batch.core.job.JobBuilder
import org.jeasy.batch.core.reader.IterableRecordReader

/**
 * Class that creates the job entity passed to the job executor. This can customized with custom batch size,
 * additional reader, processors, writers, and marshallers
 */
class JobService(private val invoiceService: InvoiceService, private val billingService: BillingService) {
    fun getPaymentProcessingBatchJob() : Job {
        val iterableRecordReader = IterableRecordReader(invoiceService.fetchPendingInvoiceIds())
        val billingWriter = BillingWriter(invoiceService, billingService)

        return JobBuilder<Int, Int>()
                .batchSize(20)
                .enableBatchScanning(true)
                .reader(iterableRecordReader)
                .writer(billingWriter)
                .build()
    }
}