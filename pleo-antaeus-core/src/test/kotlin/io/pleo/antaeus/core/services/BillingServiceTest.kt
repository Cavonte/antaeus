package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.MissingFundsException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

internal class BillingServiceTest
{
    private val solventUsCustomer = Customer(1, Currency.USD)
    private val insolventUsdCustomer = Customer(2, Currency.USD)
    private val euCustomer = Customer(3, Currency.EUR)
    private val missingCustomer = Customer(4, Currency.USD)
    private val secondMissingCustomer = Customer(5, Currency.USD)
    private val currencyMismatchCustomer = Customer(6, Currency.USD)
    private val solarCustomer = Customer(7, Currency.USD)

    private val paymentServiceMock = mockk<PaymentProvider> {
        every { charge(getValidPendingInvoice(solventUsCustomer)) } returns true
        every { charge(getValidPendingInvoice(insolventUsdCustomer)) } returns false
        every { charge(getValidPendingInvoice(currencyMismatchCustomer)) } throws CurrencyMismatchException(1, currencyMismatchCustomer.id)
        every { charge(getValidPendingInvoice(secondMissingCustomer)) } throws CustomerNotFoundException(secondMissingCustomer.id)
        every { charge(getValidPendingInvoice(solarCustomer)) } throws NetworkException()
    }

    private val customerServiceMock = mockk<CustomerService> {
        every { fetch(solventUsCustomer.id) } returns solventUsCustomer
        every { fetch(insolventUsdCustomer.id) } returns insolventUsdCustomer
        every { fetch(euCustomer.id) } returns euCustomer
        every { fetch(secondMissingCustomer.id) } returns secondMissingCustomer
        every { fetch(currencyMismatchCustomer.id) } returns currencyMismatchCustomer
        every { fetch(solarCustomer.id) } returns solarCustomer
        every { fetch(missingCustomer.id) } throws CustomerNotFoundException(missingCustomer.id)
    }

    private val invoiceServiceMock = mockk<InvoiceService> {
        every { payInvoice(getValidPendingInvoice(solventUsCustomer)) } answers { }
    }

    private val billingService = BillingService(paymentServiceMock, customerServiceMock, invoiceServiceMock)

    @Test
    fun processPayment()
    {
        val invoice = getValidPendingInvoice(solventUsCustomer)

        billingService.processPayment(invoice)

        verify { customerServiceMock.fetch(solventUsCustomer.id) }
        verify { paymentServiceMock.charge(invoice) }
        verify { invoiceServiceMock.payInvoice(invoice) }
    }

    @Test
    fun `cannot charge missing customer`()
    {
        val invoice = getValidPendingInvoice(missingCustomer)

        assertThrows<CustomerNotFoundException> {
            billingService.processPayment(invoice)
        }
        verify { customerServiceMock.fetch(missingCustomer.id) }
        verify(exactly = 0) { paymentServiceMock.charge(invoice) }
        verify(exactly = 0) { invoiceServiceMock.payInvoice(invoice) }
    }

    @Test
    fun `payment provider cannot find customer`()
    {
        val invoice = getValidPendingInvoice(secondMissingCustomer)

        assertThrows<CustomerNotFoundException> {
            billingService.processPayment(invoice)
        }

        verify { customerServiceMock.fetch(secondMissingCustomer.id) }
        verify { paymentServiceMock.charge(invoice) }
        verify(exactly = 0) { invoiceServiceMock.payInvoice(invoice) }
    }

    @Test
    fun `cannot charge with invalid currency`()
    {
        val invoice = Invoice(1, euCustomer.id, Money(BigDecimal.valueOf(20000), Currency.USD), InvoiceStatus.PENDING)


        assertThrows<CurrencyMismatchException> {
            billingService.processPayment(invoice)
        }
        verify { customerServiceMock.fetch(euCustomer.id) }
        verify(exactly = 0) { paymentServiceMock.charge(invoice) }
        verify(exactly = 0) { invoiceServiceMock.payInvoice(invoice) }
    }

    @Test
    fun `payment could not be processed due to missing funds`()
    {
        val invoice = getValidPendingInvoice(insolventUsdCustomer)

        assertThrows<MissingFundsException> {
            billingService.processPayment(invoice)
        }

        verify { customerServiceMock.fetch(insolventUsdCustomer.id) }
        verify { paymentServiceMock.charge(invoice) }
        verify(exactly = 0) { invoiceServiceMock.payInvoice(invoice) }
    }

    @Test
    fun `solar interference caused a network error`()
    {
        val invoice = getValidPendingInvoice(solarCustomer)

        assertThrows<NetworkException> {
            billingService.processPayment(invoice)
        }

        verify { customerServiceMock.fetch(solarCustomer.id) }
        verify { paymentServiceMock.charge(invoice) }
        verify(exactly = 0) { invoiceServiceMock.payInvoice(invoice) }
    }

    private fun getValidPendingInvoice(customer: Customer): Invoice
    {
        return Invoice(1, customer.id, Money(BigDecimal.valueOf(20000), customer.currency), InvoiceStatus.PENDING)
    }
}