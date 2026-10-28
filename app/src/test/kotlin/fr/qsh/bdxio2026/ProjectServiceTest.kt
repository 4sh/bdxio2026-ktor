package fr.qsh.bdxio2026

import fr.qsh.bdxio2026.services.Project
import fr.qsh.bdxio2026.services.ProjectService
import fr.qsh.bdxio2026.services.SuggestedReimbursement
import opensavvy.prepared.runner.testballoon.preparedSuite
import opensavvy.prepared.suite.assertions.checkThrows
import opensavvy.prepared.suite.assertions.log
import opensavvy.prepared.suite.prepared

val projects by prepared {
    ProjectService(
        projects = db().collection("projects"),
        transactions = db().collection("transactions"),
        reimbursements = db().collection("reimbursements"),
    )
}

val ProjectServiceTest by preparedSuite {
    suite("Project management") {
        val userA by testUser()
        val userB by testUser()
        val userC by testUser()

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

    suite("Transactions") {
        val owner by testUser()
        val memberA by testUser()
        val memberB by testUser()
        val other by testUser()

        val project by prepared {
            val id = projects().createProject(
                name = "Test project",
                currentUser = owner(),
            )

            projects().addMember(
                projectId = id,
                currentUser = owner(),
                newMember = memberA(),
            )

            projects().addMember(
                projectId = id,
                currentUser = owner(),
                newMember = memberB(),
            )

            checkNotNull(projects().getById(id, currentUser = owner()))
        }

        test("A project member can create a transaction") {
            projects().createTransaction(
                currentUser = memberA(),
                project = project()._id,
                amount = 100L,
            )

            // All members have access to the transaction
            val transactions = log(projects().findTransactions(currentUser = memberB(), project = project()._id))
            check(transactions.size == 1)
            check(transactions[0].amount == 100L)

            // A non-member doesn't have access to the transactions
            checkThrows<IllegalStateException> {
                projects().findTransactions(currentUser = other(), project = project()._id)
            }
        }

        test("A non-project member cannot create transactions") {
            checkThrows<IllegalStateException> {
                projects().createTransaction(
                    currentUser = other(),
                    project = project()._id,
                    amount = 100L,
                )
            }

            // No transaction was created
            check(projects().findTransactions(currentUser = owner(), project = project()._id).isEmpty())
        }

        suite("Total spending") {
            test("The total is 0 when no transactions exist") {
                check(projects().findTotalSpending(currentUser = owner(), project = project()._id).isEmpty())
            }

            test("Nominal case") {
                val id = project()._id

                projects().createTransaction(currentUser = memberA(), project = id, amount = 100L)
                projects().createTransaction(currentUser = memberA(), project = id, amount = 50L)
                projects().createTransaction(currentUser = memberB(), project = id, amount = 30L)

                // Any member can read the totals
                val spending = log(projects().findTotalSpending(currentUser = owner(), project = id))

                check(spending == mapOf(memberA() to 150L, memberB() to 30L))
                check(owner() !in spending) { "Users without transactions are absent from the result" }
            }

            test("The total ignores other projects") {
                val id = project()._id

                val otherProject = projects().createProject(
                    name = "Other project",
                    currentUser = memberA(),
                )

                projects().createTransaction(currentUser = memberA(), project = id, amount = 100L)
                projects().createTransaction(currentUser = memberA(), project = otherProject, amount = 999L)

                check(projects().findTotalSpending(currentUser = memberA(), project = id) == mapOf(memberA() to 100L))
            }

            test("A non-project member cannot read the total spending") {
                checkThrows<IllegalStateException> {
                    projects().findTotalSpending(currentUser = other(), project = project()._id)
                }
            }
        }
    }

    suite("Reimbursements") {
        val owner by testUser()
        val member by testUser()
        val other by testUser()

        val project by prepared {
            val id = projects().createProject(
                name = "Test project",
                currentUser = owner(),
            )

            projects().addMember(
                projectId = id,
                currentUser = owner(),
                newMember = member(),
            )

            checkNotNull(projects().getById(id, currentUser = owner()))
        }

        test("No reimbursements exist by default") {
            check(projects().findReimbursements(currentUser = owner(), project = project()._id).isEmpty())
        }

        test("A project member can create a reimbursement") {
            projects().createReimbursement(
                currentUser = member(),
                destinationUser = owner(),
                project = project()._id,
                amount = 100L,
            )

            // All members have access to the reimbursement
            val reimbursements = log(projects().findReimbursements(currentUser = owner(), project = project()._id))
            check(reimbursements.size == 1)
            check(reimbursements[0].projectId == project()._id)
            check(reimbursements[0].fromUser == member())
            check(reimbursements[0].toUser == owner())
            check(reimbursements[0].amount == 100L)

            // A non-member doesn't have access to the reimbursements
            checkThrows<IllegalStateException> {
                projects().findReimbursements(currentUser = other(), project = project()._id)
            }
        }

        test("A non-project member cannot create reimbursements") {
            checkThrows<IllegalStateException> {
                projects().createReimbursement(
                    currentUser = other(),
                    destinationUser = owner(),
                    project = project()._id,
                    amount = 100L,
                )
            }

            check(projects().findReimbursements(currentUser = owner(), project = project()._id).isEmpty())
        }

        test("Cannot reimburse a user who is not a project member") {
            checkThrows<IllegalStateException> {
                projects().createReimbursement(
                    currentUser = owner(),
                    destinationUser = other(),
                    project = project()._id,
                    amount = 100L,
                )
            }

            check(projects().findReimbursements(currentUser = owner(), project = project()._id).isEmpty())
        }

        test("Cannot reimburse yourself") {
            checkThrows<IllegalStateException> {
                projects().createReimbursement(
                    currentUser = owner(),
                    destinationUser = owner(),
                    project = project()._id,
                    amount = 100L,
                )
            }

            check(projects().findReimbursements(currentUser = owner(), project = project()._id).isEmpty())
        }

        test("The amount must be positive") {
            for (amount in listOf(0L, -100L)) {
                checkThrows<IllegalStateException> {
                    projects().createReimbursement(
                        currentUser = member(),
                        destinationUser = owner(),
                        project = project()._id,
                        amount = amount,
                    )
                }
            }

            check(projects().findReimbursements(currentUser = owner(), project = project()._id).isEmpty())
        }

        test("Reimbursements ignore other projects") {
            val otherProject = projects().createProject(
                name = "Other project",
                currentUser = owner(),
            )
            projects().addMember(projectId = otherProject, currentUser = owner(), newMember = member())

            projects().createReimbursement(currentUser = member(), destinationUser = owner(), project = project()._id, amount = 100L)
            projects().createReimbursement(currentUser = member(), destinationUser = owner(), project = otherProject, amount = 999L)

            val reimbursements = projects().findReimbursements(currentUser = owner(), project = project()._id)
            check(reimbursements.map { it.amount } == listOf(100L))
        }
    }

    suite("Suggested reimbursements") {
        val userA by testUser()
        val userB by testUser()
        val userC by testUser()
        val other by testUser()

        val project by prepared {
            val id = projects().createProject(
                name = "Test project",
                currentUser = userA(),
            )

            projects().addMember(projectId = id, currentUser = userA(), newMember = userB())
            projects().addMember(projectId = id, currentUser = userA(), newMember = userC())

            checkNotNull(projects().getById(id, currentUser = userA()))
        }

        test("Nothing to suggest without transactions") {
            check(projects().suggestReimbursements(currentUser = userA(), project = project()._id).isEmpty())
        }

        test("Nothing to suggest when spending is already even") {
            projects().createTransaction(currentUser = userA(), project = project()._id, amount = 100L)
            projects().createTransaction(currentUser = userB(), project = project()._id, amount = 100L)
            projects().createTransaction(currentUser = userC(), project = project()._id, amount = 100L)

            check(projects().suggestReimbursements(currentUser = userA(), project = project()._id).isEmpty())
        }

        test("One member overspent") {
            projects().createTransaction(currentUser = userA(), project = project()._id, amount = 100L)
            projects().createTransaction(currentUser = userB(), project = project()._id, amount = 150L)
            projects().createTransaction(currentUser = userC(), project = project()._id, amount = 50L)

            check(projects().suggestReimbursements(currentUser = userA(), project = project()._id) == listOf(SuggestedReimbursement(fromUser = userC(), toUser = userB(), amount = 50L)))
        }

        test("Existing reimbursements are taken into account") {
            projects().createTransaction(currentUser = userA(), project = project()._id, amount = 100L)
            projects().createTransaction(currentUser = userB(), project = project()._id, amount = 150L)
            projects().createTransaction(currentUser = userC(), project = project()._id, amount = 50L)

            projects().createReimbursement(currentUser = userC(), destinationUser = userB(), project = project()._id, amount = 20L)
            check(projects().suggestReimbursements(currentUser = userA(), project = project()._id) == listOf(SuggestedReimbursement(fromUser = userC(), toUser = userB(), amount = 30L)))

            projects().createReimbursement(currentUser = userC(), destinationUser = userB(), project = project()._id, amount = 30L)
            check(projects().suggestReimbursements(currentUser = userA(), project = project()._id).isEmpty())
        }

        test("Several debtors, one creditor") {
            // 100 per user
            projects().createTransaction(currentUser = userA(), project = project()._id, amount = 300L)

            val result = log(projects().suggestReimbursements(currentUser = userA(), project = project()._id))

            check(result.size == 2)
            check(result[0] == SuggestedReimbursement(fromUser = userB(), toUser = userA(), amount = 100L))
            check(result[1] == SuggestedReimbursement(fromUser = userC(), toUser = userA(), amount = 100L))
        }

        test("Remaining cents are fully settled") {
            // ~33 per user
            projects().createTransaction(currentUser = userA(), project = project()._id, amount = 100L)

            val result = log(projects().suggestReimbursements(currentUser = userA(), project = project()._id))

            // Shares are 34, 33, 33 cents: A has spent 100 and is owed 66 in total
            check(result.sumOf { it.amount } == 66L)
            check(result.all { it.toUser == userA() })
            check(result.size == 2)
        }

        test("A non-project member cannot get suggestions") {
            checkThrows<IllegalStateException> {
                projects().suggestReimbursements(currentUser = other(), project = project()._id)
            }
        }
    }
}
