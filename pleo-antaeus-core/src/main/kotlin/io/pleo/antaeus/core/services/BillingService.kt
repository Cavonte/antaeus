package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.MissingFundsException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val customerService: CustomerService,
        private val invoiceService: InvoiceService
)
{
    fun processPayment(invoice: Invoice)
    {
        val customerId = getVettedCustomerId(invoice)

        if (!paymentProvider.charge(invoice))
            throw MissingFundsException(invoice.id, customerId)

        invoiceService.payInvoice(invoice)
    }

    private fun getVettedCustomerId(invoice: Invoice): Int
    {
        val customer = customerService.fetch(invoice.customerId)

        if (invoice.amount.currency != customer.currency)
            throw CurrencyMismatchException(invoice.id, customer.id)

        return customer.id
    }
}
