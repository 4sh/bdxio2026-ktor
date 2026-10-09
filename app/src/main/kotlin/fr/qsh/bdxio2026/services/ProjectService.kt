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

}
