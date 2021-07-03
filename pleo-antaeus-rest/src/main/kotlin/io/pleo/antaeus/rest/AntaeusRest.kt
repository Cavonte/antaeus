/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.TestDataUtils
import io.pleo.antaeus.core.services.jobs.JobService
import mu.KotlinLogging
import org.jeasy.batch.core.job.JobExecutor

private val logger = KotlinLogging.logger {}
private val thisFile: () -> Unit = {}

class AntaeusRest(
        private val invoiceService: InvoiceService,
        private val customerService: CustomerService,
        private val testDataUtils: TestDataUtils,
        private val jobExecutor: JobExecutor,
        private val jobService: JobService
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
            get("/") {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            }
            path("rest") {
                // Route to check whether the app is running
                // URL: /rest/health
                get("health") {
                    it.json("ok")
                }

                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        get {
                            it.json(invoiceService.fetchAll())
                        }

                        // URL: /rest/v1/invoices/{:id}
                        get(":id") {
                            it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                        }

                        path("payment")
                        {
                            // URL: /rest/v1/invoices/payment/pending
                            // List all invoices id that are pending
                            get("pending") {
                                it.json(invoiceService.fetchPendingInvoiceIds())
                            }

                            // URL: /rest/v1/invoices/payment/cashout
                            // Force a manual run of the the billing job
                            post("cashout") {
                                val jobReport = jobExecutor.execute(jobService.getPaymentProcessingBatchJob())
                                it.json(jobReport.toString())
                            }
                        }
                    }

                    path("customers") {
                        // URL: /rest/v1/customers
                        get {
                            it.json(customerService.fetchAll())
                        }

                        // URL: /rest/v1/customers/{:id}
                        get(":id") {
                            it.json(customerService.fetch(it.pathParam("id").toInt()))
                        }
                    }

                    path("schema") {
                        // URL: /rest/v1/schema/reset
                        // Drop all the tables
                        post("reset") {
                            testDataUtils.reset()
                        }

                        // URL: /rest/v1/schema/setup
                        // Populate file db with fake invoices
                        post("setup") {
                            testDataUtils.createFakeData()
                        }
                    }
                }
            }
        }
    }
}
