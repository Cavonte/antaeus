package io.pleo.antaeus.core.exceptions

class MissingFundsException(invoiceId: Int, customerId: Int) :
    Exception("Customer '$customerId' account balance did not allow the charge. Invoice '$invoiceId'")