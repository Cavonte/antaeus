package io.pleo.antaeus.core.services

import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import kotlin.random.Random

/**
 * Util created to wipe and create fake data. Used to validate the endpoints and the schedule job
 */
class TestDataUtils(private val dal: AntaeusDal) {
    fun reset() {
        dal.reset()
    }

    fun createFakeData() {
        val customers = (1..100).mapNotNull {
            dal.createCustomer(
                    currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
            )
        }

        customers.forEach { customer ->
            (1..10).forEach {
                dal.createInvoice(
                        amount = Money(
                                value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                                currency = customer.currency
                        ),
                        customer = customer,
                        status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
                )
            }
        }
    }
}