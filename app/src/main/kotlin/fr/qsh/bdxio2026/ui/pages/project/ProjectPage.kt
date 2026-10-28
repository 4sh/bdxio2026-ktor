package fr.qsh.bdxio2026.ui.pages.project

import fr.qsh.bdxio2026.AuthCookiePayload
import fr.qsh.bdxio2026.services.Project
import fr.qsh.bdxio2026.services.ProjectService
import fr.qsh.bdxio2026.services.UserService
import fr.qsh.bdxio2026.ui.pages.formatEuros
import fr.qsh.bdxio2026.ui.pages.parseEuros
import fr.qsh.bdxio2026.ui.pages.respondPage
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.li
import kotlinx.html.ul
import opensavvy.ktmongo.bson.types.ObjectId

fun routeProject(id: ObjectId) = "/projects/${id.hex}"

fun routeProjectMembers(id: ObjectId) = "${routeProject(id)}/members"

fun routeProjectTransactions(id: ObjectId) = "${routeProject(id)}/transactions"

fun routeProjectReimbursements(id: ObjectId) = "${routeProject(id)}/reimbursements"

fun Route.viewProject(
    projects: ProjectService,
    users: UserService,
) {

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
        newMemberUsername: String? = null,
        newMemberUnknown: Boolean = false,
        newTransactionAmount: String? = null,
        newTransactionAmountInvalid: Boolean = false,
        newReimbursementAmount: String? = null,
        newReimbursementAmountInvalid: Boolean = false,
    ) {
        val usernames = users.getUsernames(project.members)

        val usernameOf = { member: ObjectId -> usernames[member] ?: "Unknown user" }

        val transactions = projects.findTransactions(currentUser, project._id)
        val spending = projects.findTotalSpending(currentUser, project._id)
        val reimbursements = projects.findReimbursements(currentUser, project._id)
        val suggestions = projects.suggestReimbursements(currentUser, project._id)

        val transactionsTotal = transactions.sumOf { it.amount }
        val transactionsAverage = transactionsTotal / project.members.size

        call.respondPage(
            title = project.name,
            status = when {
                newMemberUnknown || newTransactionAmountInvalid || newReimbursementAmountInvalid -> HttpStatusCode.UnprocessableEntity
                else -> HttpStatusCode.OK
            }
        ) {
            h1 {
                +project.name
            }

            projectMembers(
                project = project,
                currentUser = currentUser,
                newMemberUsername = newMemberUsername,
                newMemberUnknown = newMemberUnknown,
                usernameOf = usernameOf,
            )

            h2 {
                +"Transactions"
            }

            projectTransactions(
                project = project,
                transactions = transactions,
                transactionsTotal = transactionsTotal,
                transactionsAverage = transactionsAverage,
                usernameOf = usernameOf,
                newTransactionAmount = newTransactionAmount,
                newTransactionAmountInvalid = newTransactionAmountInvalid,
            )

            h2 {
                +"Total spending"
            }

            ul {
                for (member in project.members) {
                    li {
                        +"${usernameOf(member)}: ${formatEuros(spending[member] ?: 0)}"
                    }
                }
            }

            if (project.members.size > 1) {
                h2 {
                    +"Reimbursements"
                }

                projectReimbursements(
                    project = project,
                    reimbursements = reimbursements,
                    suggestions = suggestions,
                    currentUser = currentUser,
                    usernameOf = usernameOf,
                    newReimbursementAmount = newReimbursementAmount,
                    newReimbursementAmountInvalid = newReimbursementAmountInvalid,
                )
            }
        }
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
            respondProjectPage(project, user.userId, newMemberUsername = username, newMemberUnknown = true)
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
            respondProjectPage(project, user.userId, newTransactionAmount = rawAmount, newTransactionAmountInvalid = true)
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
                newReimbursementAmount = rawAmount,
                newReimbursementAmountInvalid = true
            )
            return@post
        }

        projects.createReimbursement(user.userId, destination, project._id, amount)

        call.respondRedirect(routeProject(project._id))
    }
}
