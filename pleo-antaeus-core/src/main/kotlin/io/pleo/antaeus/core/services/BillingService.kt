package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.MissingFundsException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val customerService: CustomerService,
        private val invoiceService: InvoiceService
) {
    fun processPayment(invoice: Invoice) {
        val customerId = getVettedCustomerId(invoice)

        if (!paymentProvider.charge(invoice)) {
            logger.error { "Missing fund when processing payment customer '$customerId' and invoice '${invoice.id}'." }
            throw MissingFundsException(invoice.id, customerId)
        }

        invoiceService.updatePaymentStatus(invoice, InvoiceStatus.PAID)
    }

    /*
    Validates customer exists and that the currencies match
     */
    private fun getVettedCustomerId(invoice: Invoice): Int {
        val customer = customerService.fetch(invoice.customerId)

        if (invoice.amount.currency != customer.currency) {
            logger.error { "Currency mismatch for customer '${customer.id}' and invoice '${invoice.id}." }
            throw CurrencyMismatchException(invoice.id, customer.id)
        }

        return customer.id
    }
}
