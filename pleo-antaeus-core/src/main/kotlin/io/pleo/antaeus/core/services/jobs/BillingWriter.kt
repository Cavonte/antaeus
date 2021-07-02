package io.pleo.antaeus.core.services.jobs

import io.pleo.antaeus.core.exceptions.MissingFundsException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import mu.KotlinLogging
import org.jeasy.batch.core.record.Batch
import org.jeasy.batch.core.writer.RecordWriter

private val logger = KotlinLogging.logger {}

class BillingWriter(private val invoiceService: InvoiceService, private val billingService: BillingService) : RecordWriter<Int> {
    override fun writeRecords(batch: Batch<Int>?) {
        if (batch != null && !batch.isEmpty) {
            logger.info { "New batch." }
            batch.forEach { invoiceIdRecord ->
                val invoice = invoiceService.fetch(invoiceIdRecord.payload)
                if (!invoice.isPaid()) {
                    logger.info { "Processing invoice '${invoice.id}'" }
                    try {
                        billingService.processPayment(invoice)
                    } catch (exception: MissingFundsException) {
                        //not fatal enough for job to fail. Error/Notice should be raised however
                        logger.error { "Error processing payment for invoice '${invoice.id}. ${exception.message}" }
                    }
                }
                else {
                    logger.error { "Skipping paid invoice '${invoice.id}'" }
                }
            }
        }
    }
}