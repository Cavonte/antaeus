package io.pleo.antaeus.app.batch

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
                    logger.info { "Processing invoice '${invoice.id}" }
                    billingService.processPayment(invoice)
                }
                else {
                    logger.error { "Skipping paid invoice '${invoice.id}" }
                }
            }
        }
    }
}