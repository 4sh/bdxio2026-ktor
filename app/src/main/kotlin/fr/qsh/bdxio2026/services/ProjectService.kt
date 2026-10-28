package fr.qsh.bdxio2026.services

import kotlinx.serialization.Serializable
import opensavvy.ktmongo.api.toList
import opensavvy.ktmongo.bson.types.ObjectId
import opensavvy.ktmongo.coroutines.CoroutineMongoCollection

@Serializable
data class Project(
    val _id: ObjectId,
    val name: String,
    val creator: ObjectId,
    val members: List<ObjectId>,
)

@Serializable
data class Transaction(
    val _id: ObjectId,
    val projectId: ObjectId,

    /**
     * User who created the transaction.
     */
    val userId: ObjectId,

    /**
     * Stored in euro cents to avoid floating point imprecision.
     */
    val amount: Long,
)

@Serializable
data class Reimbursement(
    val _id: ObjectId,
    val projectId: ObjectId,
    val fromUser: ObjectId,
    val toUser: ObjectId,
    val amount: Long,
)

@Serializable
data class AmountPerUser(
    val _id: ObjectId, // User ID
    val balance: Long,
)

data class SuggestedReimbursement(
    val fromUser: ObjectId,
    val toUser: ObjectId,
    val amount: Long,
)

class ProjectService(
    private val projects: CoroutineMongoCollection<Project>,
    private val transactions: CoroutineMongoCollection<Transaction>,
    private val reimbursements: CoroutineMongoCollection<Reimbursement>,
) {

    suspend fun createProject(
        name: String,
        currentUser: ObjectId,
    ): ObjectId {
        val id = projects.newId()

        projects.insertOne(
            Project(
                _id = id,
                name = name,
                creator = currentUser,
                members = listOf(currentUser),
            )
        )

        return id
    }

    suspend fun getById(
        id: ObjectId,
        currentUser: ObjectId,
    ): Project? = projects.findOne {
        Project::_id eq id
        Project::members.any eq currentUser
    }

    /**
     * Adds [newMember] to the project. Only the creator can do it.
     *
     * @return `false` if the project doesn't exist or [currentUser] isn't its creator.
     */
    @IgnorableReturnValue
    suspend fun addMember(
        projectId: ObjectId,
        currentUser: ObjectId,
        newMember: ObjectId,
    ): Boolean {
        val project = projects.findOne {
            Project::_id eq projectId
            Project::creator eq currentUser
        } ?: return false

        if (newMember in project.members)
            return true

        projects.filter { Project::_id eq projectId }
            .updateOne {
                Project::members push newMember
            }

        return true
    }

    suspend fun getMine(
        currentUser: ObjectId,
    ): List<Project> = projects.find {
        Project::members.any eq currentUser
    }.toList()

    suspend fun createTransaction(
        currentUser: ObjectId,
        project: ObjectId,
        amount: Long,
    ) {
        check(getById(project, currentUser) != null) { "You are not allowed to access this project" }

        val id = transactions.newId()

        transactions.insertOne(
            Transaction(
                _id = id,
                projectId = project,
                userId = currentUser,
                amount = amount,
            )
        )
    }

    suspend fun findTransactions(
        currentUser: ObjectId,
        project: ObjectId,
    ): List<Transaction> {
        check(getById(project, currentUser) != null) { "You are not allowed to access this project" }

        return transactions.find({
            sort {
                descending(Transaction::_id)
            }
        }) {
            Transaction::projectId eq project
        }.toList()
    }

    suspend fun findTotalSpending(
        currentUser: ObjectId,
        project: ObjectId,
    ): Map<ObjectId, Long> {
        check(getById(project, currentUser) != null) { "You are not allowed to access this project" }

        return transactions.aggregate()
            .match { Transaction::projectId eq project }
            .group {
                AmountPerUser::_id set Transaction::userId
                AmountPerUser::balance sum Transaction::amount
            }
            .toList()
            .associate { it._id to it.balance }
    }

    suspend fun createReimbursement(
        currentUser: ObjectId,
        destinationUser: ObjectId,
        project: ObjectId,
        amount: Long,
    ) {
        val projectValue = getById(project, currentUser)
        checkNotNull(projectValue) { "You are not allowed to access this project" }
        check(destinationUser in projectValue.members) { "The destination user is not a member of this project" }
        check(destinationUser != currentUser) { "You cannot reimburse yourself" }
        check(amount > 0) { "The amount must be positive" }

        reimbursements.insertOne(
            Reimbursement(
                _id = reimbursements.newId(),
                projectId = project,
                fromUser = currentUser,
                toUser = destinationUser,
                amount = amount,
            )
        )
    }

    suspend fun findReimbursements(
        currentUser: ObjectId,
        project: ObjectId,
    ): List<Reimbursement> {
        checkNotNull(getById(project, currentUser)) { "You are not allowed to access this project" }

        return reimbursements.find {
            Reimbursement::projectId eq project
        }.toList()
    }

    /**
     * Suggests reimbursements that even the spending of every member of the [project].
     */
    suspend fun suggestReimbursements(
        currentUser: ObjectId,
        project: ObjectId,
    ): List<SuggestedReimbursement> {
        val projectValue = getById(project, currentUser)
        checkNotNull(projectValue) { "You are not allowed to access this project" }

        val spending = findTotalSpending(currentUser, project)
        val done = findReimbursements(currentUser, project)

        val members = projectValue.members
        val total = spending.values.sum()
        val base = total / members.size
        val remainder = (total % members.size).toInt()

        val balances = members.mapIndexed { index, member ->
            val share = if (index < remainder) base + 1 else base
            val sent = done.filter { it.fromUser == member }.sumOf { it.amount }
            val received = done.filter { it.toUser == member }.sumOf { it.amount }

            member to (spending[member] ?: 0L) + sent - received - share
        }

        return settleBalances(balances)
    }

    /**
     * Computes suggested reimbursements.
     *
     * @param balances The balances of each member.
     * The [ObjectId] represents the member's ID, the [Long] represents their balance: if positive, they are owed money, if negative, they owe money.
     */
    private fun settleBalances(balances: List<Pair<ObjectId, Long>>): List<SuggestedReimbursement> {
        val debtors = balances.filter { it.second < 0 }.map { it.first to -it.second }.toMutableList()
        val creditors = balances.filter { it.second > 0 }.toMutableList()
        val result = ArrayList<SuggestedReimbursement>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            debtors.sortByDescending { it.second }
            creditors.sortByDescending { it.second }

            val (debtor, owed) = debtors.removeFirst()
            val (creditor, claimed) = creditors.removeFirst()
            val amount = minOf(owed, claimed)

            result += SuggestedReimbursement(fromUser = debtor, toUser = creditor, amount = amount)

            if (owed > amount) debtors += debtor to (owed - amount)
            if (claimed > amount) creditors += creditor to (claimed - amount)
        }

        return result
    }

}
