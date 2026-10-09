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
    }

}
