package io.pleo.antaeus.app.batch

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService

class JobCreator(private val invoiceService: InvoiceService,
                 private val billingService: BillingService)
{

}