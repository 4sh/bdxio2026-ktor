package fr.qsh.bdxio2026

import fr.qsh.bdxio2026.services.Project
import fr.qsh.bdxio2026.services.ProjectService
import opensavvy.prepared.runner.testballoon.preparedSuite
import opensavvy.prepared.suite.assertions.log
import opensavvy.prepared.suite.prepared

val projects by prepared {
    ProjectService(
        projects = db().collection("projects"),
    )
}

val ProjectServiceTest by preparedSuite {

    val userA by testUser()
    val userB by testUser()
    val userC by testUser()

    suite("Project management") {
        test("Create a project") {
            val id = projects().createProject(
                name = "Test project",
                currentUser = userA(),
            )

            val project = log(projects().getById(id, userA()))

            check(project?.name == "Test project")
            check(project.creator == userA())
            check(project.members == listOf(userA()))

            check(projects().getMine(userA()) == listOf(project))
        }

        test("A user cannot access projects that don't belong to them") {
            val id = projects().createProject(
                name = "Test project",
                currentUser = userA(),
            )

            check(projects().getById(id, userB()) == null)
            check(projects().getMine(userB()) == emptyList<Project>())
        }

        test("Share a project with another user") {
            val owner = userA()
            val member = userB()
            val other = userC()

            val id = projects().createProject(
                name = "Test project",
                currentUser = owner,
            )

            val added = projects().addMember(
                projectId = id,
                currentUser = owner,
                newMember = member,
            )
            check(added)

            check(projects().getById(id, currentUser = owner) != null) { "The owner can see their own project" }
            check(projects().getById(id, currentUser = member) != null) { "A member can see projects they are a member of" }
            check(projects().getById(id, currentUser = other) == null) { "Another user cannot see projects they are not a member of" }

            check(projects().getMine(currentUser = owner).size == 1)
            check(projects().getMine(currentUser = member).size == 1)
            check(projects().getMine(currentUser = other).isEmpty())
        }

        test("Cannot share a project you are not a member of") {
            val owner = userA()
            val fakeMember = userB()
            val other = userC()

            val id = projects().createProject(
                name = "Test project",
                currentUser = owner,
            )

            check(!projects().addMember(projectId = id, currentUser = other, newMember = fakeMember))

            // Check that this did not change the access rights
            check(projects().getById(id, currentUser = fakeMember) == null)
            check(projects().getMine(currentUser = fakeMember).isEmpty())
        }

        test("Cannot share a project you are not the creator of") {
            val owner = userA()
            val member = userB()
            val other = userC()

            val id = projects().createProject(
                name = "Test project",
                currentUser = owner,
            )

            // Invite the member for real
            check(projects().addMember(projectId = id, currentUser = owner, newMember = member))

            // The member attempts to invite a third member
            check(!projects().addMember(projectId = id, currentUser = member, newMember = other))

            // Check that this did not change the access rights
            check(projects().getById(id, currentUser = other) == null)
            check(projects().getMine(currentUser = other).isEmpty())
        }
    }

}
