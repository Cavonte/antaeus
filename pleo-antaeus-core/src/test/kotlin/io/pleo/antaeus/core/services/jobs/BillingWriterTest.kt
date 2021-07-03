package io.pleo.antaeus.core.services.jobs

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.MissingFundsException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.*
import org.jeasy.batch.core.record.Batch
import org.jeasy.batch.core.record.GenericRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

internal class BillingWriterTest {
    private val usdInvoice = getValidPendingInvoice(Customer(1, Currency.USD))
    private val dkkInvoice = getValidPendingInvoice(Customer(2, Currency.DKK))
    private val euInvoice = getValidPendingInvoice(Customer(3, Currency.EUR))
    private val paidInvoice = getValidPaidInvoice(Customer(4, Currency.EUR))
    private val missingInvoice = getValidPaidInvoice(Customer(5, Currency.EUR))
    private val missingFundsInvoice = getValidPendingInvoice(Customer(6, Currency.EUR))

    private val invoiceServiceMock = mockk<InvoiceService> {
        every { fetch(usdInvoice.id) } returns usdInvoice
        every { fetch(dkkInvoice.id) } returns dkkInvoice
        every { fetch(euInvoice.id) } returns euInvoice
        every { fetch(paidInvoice.id) } returns paidInvoice
        every { fetch(missingInvoice.id) } throws InvoiceNotFoundException(6)
        every { fetch(missingFundsInvoice.id) } returns missingFundsInvoice
    }

    private val billingServiceMock = mockk<BillingService> {
        every { processPayment(usdInvoice) } answers { }
        every { processPayment(dkkInvoice) } answers { }
        every { processPayment(euInvoice) } answers { }
        every { processPayment(missingFundsInvoice) } throws MissingFundsException(missingFundsInvoice.id, missingFundsInvoice.customerId)
    }

    private val billingWriter = BillingWriter(invoiceServiceMock, billingServiceMock)

    @Test
    fun writeRecords() {
        billingWriter.writeRecords(Batch(listOf(GenericRecord(null, usdInvoice.id), GenericRecord(null, dkkInvoice.id), GenericRecord(null, euInvoice.id))))

        verify(exactly = 3) { invoiceServiceMock.fetch(any()) }
        verify(exactly = 3) { billingServiceMock.processPayment(any()) }
    }

    @Test
    fun `process only unpaid invoices`() {
        billingWriter.writeRecords(Batch(listOf(GenericRecord(null, euInvoice.id), GenericRecord(null, paidInvoice.id))))

        verify { invoiceServiceMock.fetch(euInvoice.id) }
        verify { invoiceServiceMock.fetch(paidInvoice.id) }
        verify { billingServiceMock.processPayment(euInvoice) }
        verify(exactly = 0) { billingServiceMock.processPayment(paidInvoice) }
    }

    @Test
    fun `missing invoice breaks jobs`() {
        assertThrows<InvoiceNotFoundException> {
            billingWriter.writeRecords(Batch(listOf(GenericRecord(null, paidInvoice.id), GenericRecord(null, missingInvoice.id))))
        }
        verify { invoiceServiceMock.fetch(paidInvoice.id) }
        verify { invoiceServiceMock.fetch(missingInvoice.id) }
        verify(exactly = 0) { billingServiceMock.processPayment(any()) }
    }

    @Test
    fun `missing funds does not break job`() {
        billingWriter.writeRecords(Batch(listOf(GenericRecord(null, paidInvoice.id), GenericRecord(null, missingFundsInvoice.id))))
        verify { invoiceServiceMock.fetch(paidInvoice.id) }
        verify { invoiceServiceMock.fetch(missingFundsInvoice.id) }
        verify { billingServiceMock.processPayment(missingFundsInvoice) }
    }


    private fun getValidPendingInvoice(customer: Customer): Invoice {
        return Invoice(customer.id, customer.id, Money(BigDecimal.valueOf(112), customer.currency), InvoiceStatus.PENDING)
    }

    private fun getValidPaidInvoice(customer: Customer): Invoice {
        return Invoice(customer.id, customer.id, Money(BigDecimal.valueOf(2424), customer.currency), InvoiceStatus.PAID)
    }
}

