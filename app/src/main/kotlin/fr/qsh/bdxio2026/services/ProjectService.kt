package fr.qsh.bdxio2026.services

import kotlinx.serialization.Serializable
import opensavvy.ktmongo.bson.types.ObjectId
import opensavvy.ktmongo.coroutines.CoroutineMongoCollection

@Serializable
data class Project(
    val _id: ObjectId,
    val name: String,
    val creator: ObjectId,
    val members: List<ObjectId>,
)

class ProjectService(
    private val projects: CoroutineMongoCollection<Project>,
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

    suspend fun getMine(
        currentUser: ObjectId,
    ): List<Project> = projects.find {
        Project::members.any eq currentUser
    }.toList()

}
