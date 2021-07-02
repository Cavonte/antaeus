##Proposed Solution

Invoice processing does not need to be instant like other online operations or customer facing features. It can be
handled by a batching system like video processing or model training. The invoice processing job is to be run at a fixed schedule
i.e. First Day of the month.

Alternatively added endpoints can be called to trigger the job.

The core of the changes are in the pleo-antaeus-core/services module, adn the main app in pleo-antaeus-app 
   - BillingService receives, charge payment and process invoices. Handles edge cases and possible issues with currencies, invoices and customer
   - JobService and BillingWriter are batch utilities that will process and use BillingService
   - AnteusApp.kt initializes the additional services, and sets on the job scheduling.
   
###Why batch processing
[Batch processing](https://javaee.github.io/tutorial/batch-processing001.html) is better suited for processing large amounts data periodically. 
I think this is good use case. Using a batch framework will give flexibility in handling the business logic, error handling,
keeping track of errors, metrics etc.

For this project I evaluated Easy Batch and Spring Batch. I picked Easy Batch for the ease of configuration, the easier learning curve
and the lack of boilerplate. (also maybe time constraints).
In an enterprise context, Spring batch will provide way more options and modularity.

[Easy Batch](https://github.com/j-easy/easy-batch/tree/master/easy-batch-tutorials)

Side by side comparison [here.](https://github.com/benas/easy-batch-vs-spring-batch/issues/1)

### Considerations
#### Scheduling
Given the importance of running this job at a fixed date. Using the built-in java scheduler would not cut it. For this challenge's purpose I used.
```
scheduler.scheduleAtFixedRate({ jobExecutor.execute(jobService.getPaymentProcessingBatchJob()) }, 60, 45, TimeUnit.SECONDS)
```
In a real world situation the scheduling should be handled by an external framework or be stateful (job backup and restoration).
 e.g. Quartz. Depending on this  application alone might cause scheduled jobs to be lost, repeated or not run at all.
#### Concurrency and Error Handling when dealing with financial data
Since this service is dealing with currencies, there cannot be any mistakes when managing the customer funds and invoices.
```
fun processPayment(invoice: Invoice)
```
The billings service should work with a transaction system (e.g. Spring Transaction annotation) so that operations can be retried or rolled back.
This is also important for the Batch Processing framework as incomplete jobs can be retried and completed. Jobs that are
being processed can be rolled back or aborted if any concurrency issues arise.

####Neat Easy batch features
Easy Batch allows you to create custom readers, processors and writers. 
- With a custom processor I could add additional business
logic related to payment and pass the resulting data further in the job pipeline. Or implement a custom reader,
so I don't have the 10 trillions invoices in memory.
- An option allows you to retry the smaller parts of a job if an error occurs. The entire job does not have to fail.
- Custom batch sizes.

#### Bonus
Added a github action file that could be added to the main repo. Next challengers can build and test their pr on their fork.

####Challenge
This took me a days worth of work spread over 3 days.
I spent a decent chunk of that time getting used to systems I had not used so far, Exposed, Easy Batch.
_____________________________________________________________________________________________________
_____________________________________________________________________________________________________
_____________________________________________________________________________________________________
## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
