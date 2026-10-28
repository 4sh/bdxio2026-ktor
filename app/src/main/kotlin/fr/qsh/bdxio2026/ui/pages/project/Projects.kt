package fr.qsh.bdxio2026.ui.pages.project

import fr.qsh.bdxio2026.AUTH
import fr.qsh.bdxio2026.AuthCookiePayload
import fr.qsh.bdxio2026.services.Project
import fr.qsh.bdxio2026.services.ProjectService
import fr.qsh.bdxio2026.services.UserService
import fr.qsh.bdxio2026.ui.pages.respondPage
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import opensavvy.ktmongo.bson.types.ObjectId
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

const val ROUTE_PROJECT_NEW = "/projects/new"

fun routeProject(id: ObjectId) = "/projects/${id.hex}"

fun routeProjectMembers(id: ObjectId) = "${routeProject(id)}/members"

fun routeProjectTransactions(id: ObjectId) = "${routeProject(id)}/transactions"

fun routeProjectReimbursements(id: ObjectId) = "${routeProject(id)}/reimbursements"

private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)

private fun formatEuros(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val euros = (cents / 100).absoluteValue
    val remainder = (cents % 100).absoluteValue
    return "$sign$euros.${remainder.toString().padStart(2, '0')}\u00A0€"
}

private fun parseEuros(text: String): Long? = try {
    val euros = BigDecimal(text.trim())
    if (euros.stripTrailingZeros().scale() > 2) null
    else euros.movePointRight(2).longValueExact()
} catch (_: NumberFormatException) {
    null
} catch (_: ArithmeticException) {
    null
}

fun Routing.projectPages(
    projects: ProjectService,
    users: UserService,
) {
    suspend fun RoutingContext.respondNewProjectPage(
        name: String?,
        blankName: Boolean = false,
    ) {
        call.respondPage(
            title = "New project",
            status = when {
                blankName -> HttpStatusCode.UnprocessableEntity
                else -> HttpStatusCode.OK
            }
        ) {
            h1 {
                +"New project"
            }

            form(
                action = ROUTE_PROJECT_NEW,
                method = FormMethod.post,
                encType = FormEncType.applicationXWwwFormUrlEncoded
            ) {
                p {
                    label {
                        +"Name: "
                        input(InputType.text, name = "name") {
                            required = true
                            autoFocus = true

                            if (name != null)
                                value = name
                        }
                    }

                    if (blankName)
                        +" The name cannot be blank"
                }

                input(InputType.submit)
            }
        }
    }

    suspend fun RoutingContext.findProject(currentUser: ObjectId): Project? {
        val id = try {
            ObjectId(call.parameters["id"]!!)
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.NotFound)
            return null
        }

        val project = projects.getById(id, currentUser)

        if (project == null)
            call.respond(HttpStatusCode.NotFound)

        return project
    }

    suspend fun RoutingContext.respondProjectPage(
        project: Project,
        currentUser: ObjectId,
        username: String? = null,
        unknownUser: Boolean = false,
        amount: String? = null,
        invalidAmount: Boolean = false,
        reimbursementAmount: String? = null,
        invalidReimbursement: Boolean = false,
    ) {
        val usernames = users.getUsernames(project.members)
        val transactions = projects.findTransactions(currentUser, project._id)
        val spending = projects.findTotalSpending(currentUser, project._id)
        val reimbursements = projects.findReimbursements(currentUser, project._id)
        val suggestions = projects.suggestReimbursements(currentUser, project._id)

        val total = transactions.sumOf { it.amount }
        val average = total / project.members.size

        // Positive: the member still owes money. Negative: the member is owed money.
        fun amountDue(member: ObjectId): Long {
            val sent = reimbursements.filter { it.fromUser == member }.sumOf { it.amount }
            val received = reimbursements.filter { it.toUser == member }.sumOf { it.amount }
            return average - (spending[member] ?: 0) - sent + received
        }

        call.respondPage(
            title = project.name,
            status = when {
                unknownUser || invalidAmount || invalidReimbursement -> HttpStatusCode.UnprocessableEntity
                else -> HttpStatusCode.OK
            }
        ) {
            h1 {
                +project.name
            }

            h2 {
                +"Members"
            }

            ul {
                for (member in project.members) {
                    li {
                        +(usernames[member] ?: "Unknown user")
                    }
                }
            }

            if (project.creator == currentUser) {
                form(
                    action = routeProjectMembers(project._id),
                    method = FormMethod.post,
                    encType = FormEncType.applicationXWwwFormUrlEncoded
                ) {
                    p {
                        label {
                            +"Username: "
                            input(InputType.text, name = "username") {
                                required = true

                                if (username != null)
                                    value = username
                            }
                        }

                        if (unknownUser)
                            +" No such user"
                    }

                    input(InputType.submit) {
                        value = "Add member"
                    }
                }
            }

            h2 {
                +"Transactions"
            }

            if (transactions.isEmpty()) {
                p {
                    +"No transactions yet."
                }
            } else {
                ul {
                    for (transaction in transactions) {
                        li {
                            val date = dateTimeFormat.format(Instant.ofEpochSecond(transaction._id.timestamp.epochSeconds))
                            +"$date (UTC) – ${usernames[transaction.userId] ?: "Unknown user"}: ${formatEuros(transaction.amount)}"
                        }
                    }
                }

                p {
                    +"Total: ${formatEuros(total)} (per user: ~${formatEuros(average)})"
                }
            }

            form(
                action = routeProjectTransactions(project._id),
                method = FormMethod.post,
                encType = FormEncType.applicationXWwwFormUrlEncoded
            ) {
                p {
                    label {
                        +"Amount (€): "
                        input(InputType.number, name = "amount") {
                            required = true
                            step = "0.01"

                            if (amount != null)
                                value = amount
                        }
                    }

                    if (invalidAmount)
                        +" Invalid amount"
                }

                input(InputType.submit) {
                    value = "Add transaction"
                }
            }

            h2 {
                +"Total spending"
            }

            ul {
                for (member in project.members) {
                    li {
                        +"${usernames[member] ?: "Unknown user"}: ${formatEuros(spending[member] ?: 0)}"
                    }
                }
            }

            if (project.members.size > 1) {
                h2 {
                    +"Reimbursements"
                }

                ul {
                    for (reimbursement in reimbursements) {
                        li {
                            +"${usernames[reimbursement.fromUser] ?: "Unknown user"} reimbursed ${usernames[reimbursement.toUser] ?: "Unknown user"}: ${formatEuros(reimbursement.amount)}"
                        }
                    }
                }

                ul {
                    for (member in project.members) {
                        li {
                            +"${usernames[member] ?: "Unknown user"}: due ${formatEuros(amountDue(member))}"
                        }
                    }
                }

                h3 {
                    +"Suggested reimbursements"
                }

                if (suggestions.isEmpty()) {
                    p {
                        +"Nothing to reimburse."
                    }
                } else {
                    ul {
                        for (suggestion in suggestions) {
                            li {
                                +"${usernames[suggestion.fromUser] ?: "Unknown user"} → ${usernames[suggestion.toUser] ?: "Unknown user"}: ${formatEuros(suggestion.amount)}"
                            }
                        }
                    }
                }

                form(
                    action = routeProjectReimbursements(project._id),
                    method = FormMethod.post,
                    encType = FormEncType.applicationXWwwFormUrlEncoded
                ) {
                    p {
                        label {
                            +"Reimburse: "
                            select {
                                name = "to"
                                required = true

                                for (member in project.members) {
                                    if (member != currentUser) {
                                        option {
                                            value = member.hex
                                            +(usernames[member] ?: "Unknown user")
                                        }
                                    }
                                }
                            }
                        }

                        +" "

                        label {
                            +"Amount (€): "
                            input(InputType.number, name = "amount") {
                                required = true
                                step = "0.01"
                                min = "0.01"

                                if (reimbursementAmount != null)
                                    value = reimbursementAmount
                            }
                        }

                        if (invalidReimbursement)
                            +" Invalid reimbursement"
                    }

                    input(InputType.submit) {
                        value = "Reimburse"
                    }
                }
            }
        }
    }

    authenticate(AUTH) {
        get(ROUTE_PROJECT_NEW) {
            respondNewProjectPage(name = null)
        }

        post(ROUTE_PROJECT_NEW) {
            val user = call.principal<AuthCookiePayload>()!!
            val name = call.receiveParameters()["name"]!!.trim()

            if (name.isEmpty()) {
                respondNewProjectPage(name = name, blankName = true)
                return@post
            }

            val id = projects.createProject(name, user.userId)

            call.respondRedirect(routeProject(id))
        }

        get("/projects/{id}") {
            val user = call.principal<AuthCookiePayload>()!!
            val project = findProject(user.userId) ?: return@get

            respondProjectPage(project, user.userId)
        }

        post("/projects/{id}/members") {
            val user = call.principal<AuthCookiePayload>()!!
            val project = findProject(user.userId) ?: return@post

            if (project.creator != user.userId) {
                call.respond(HttpStatusCode.Forbidden)
                return@post
            }

            val username = call.receiveParameters()["username"]!!.trim()
            val newMember = if (username.isEmpty()) null else users.findByUsername(username)

            if (newMember == null) {
                respondProjectPage(project, user.userId, username = username, unknownUser = true)
                return@post
            }

            projects.addMember(project._id, user.userId, newMember)

            call.respondRedirect(routeProject(project._id))
        }

        post("/projects/{id}/transactions") {
            val user = call.principal<AuthCookiePayload>()!!
            val project = findProject(user.userId) ?: return@post

            val rawAmount = call.receiveParameters()["amount"]?.trim().orEmpty()
            val amount = parseEuros(rawAmount)

            if (amount == null) {
                respondProjectPage(project, user.userId, amount = rawAmount, invalidAmount = true)
                return@post
            }

            projects.createTransaction(user.userId, project._id, amount)

            call.respondRedirect(routeProject(project._id))
        }

        post("/projects/{id}/reimbursements") {
            val user = call.principal<AuthCookiePayload>()!!
            val project = findProject(user.userId) ?: return@post

            val parameters = call.receiveParameters()
            val rawAmount = parameters["amount"]?.trim().orEmpty()
            val amount = parseEuros(rawAmount)
            val destination = try {
                ObjectId(parameters["to"].orEmpty())
            } catch (_: IllegalArgumentException) {
                null
            }

            if (amount == null || amount <= 0 || destination == null ||
                destination == user.userId || destination !in project.members
            ) {
                respondProjectPage(
                    project,
                    user.userId,
                    reimbursementAmount = rawAmount,
                    invalidReimbursement = true
                )
                return@post
            }

            projects.createReimbursement(user.userId, destination, project._id, amount)

            call.respondRedirect(routeProject(project._id))
        }
    }
}
