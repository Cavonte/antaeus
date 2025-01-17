/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import getPaymentProvider
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.TestDataUtils
import io.pleo.antaeus.core.services.jobs.JobService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.rest.AntaeusRest
import org.jeasy.batch.core.job.JobExecutor
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import setupInitialData
import java.io.File
import java.sql.Connection
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val scheduler = Executors.newScheduledThreadPool(1)

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
            .connect(url = "jdbc:sqlite:${dbFile.absolutePath}",
                    driver = "org.sqlite.JDBC",
                    user = "root",
                    password = "")
            .also {
                TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                transaction(it) {
                    addLogger(StdOutSqlLogger)
                    // Drop all existing tables to ensure a clean slate on each run
                    SchemaUtils.drop(*tables)
                    // Create all tables
                    SchemaUtils.create(*tables)
                }
            }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Get third parties
    val paymentProvider = getPaymentProvider()

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    //service to wipe and create fake data
    val testDataUtils = TestDataUtils(dal = dal)

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(paymentProvider, customerService, invoiceService)

    //batch service and dependencies
    val jobService = JobService(invoiceService, billingService)
    val jobExecutor = JobExecutor()

    //Retry Pending invoices after a delay
    //Would normally be run once a month or more if failed invoices are to be retried
    scheduler.scheduleAtFixedRate({ jobExecutor.execute(jobService.getPaymentProcessingBatchJob()) }, 60, 45, TimeUnit.SECONDS)

    // Create REST web service
    AntaeusRest(
            invoiceService = invoiceService,
            customerService = customerService,
            testDataUtils = testDataUtils,
            jobService = jobService,
            jobExecutor = jobExecutor
    ).run()

    //job executor should be shut down when the apps shut down
    //jobExecutor.shutdown()
}
