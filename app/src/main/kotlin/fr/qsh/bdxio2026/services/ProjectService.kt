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
data class AmountPerUser(
    val _id: ObjectId, // User ID
    val balance: Long,
)

class ProjectService(
    private val projects: CoroutineMongoCollection<Project>,
    private val transactions: CoroutineMongoCollection<Transaction>,
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

}
