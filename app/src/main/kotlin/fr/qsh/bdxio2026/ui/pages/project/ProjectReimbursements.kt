package fr.qsh.bdxio2026.ui.pages.project

import fr.qsh.bdxio2026.services.Project
import fr.qsh.bdxio2026.services.Reimbursement
import fr.qsh.bdxio2026.services.SuggestedReimbursement
import fr.qsh.bdxio2026.ui.pages.formatEuros
import kotlinx.html.*
import opensavvy.ktmongo.bson.types.ObjectId

fun DIV.projectReimbursements(
    /**
     * The project to display.
     */
    project: Project,

    /**
     * The ID of the current user.
     */
    currentUser: ObjectId,

    /**
     * The reimbursements of the project.
     */
    reimbursements: List<Reimbursement>,

    /**
     * The suggested reimbursements of the project.
     */
    suggestions: List<SuggestedReimbursement>,

    /**
     * Computes a user's username based on their ID.
     */
    usernameOf: (ObjectId) -> String,

    /**
     * If non-`null`, the amount specified by the user during their previous visit to this page.
     */
    newReimbursementAmount: String?,

    /**
     * `true` to display an error when [newReimbursementAmount] cannot be parsed.
     */
    newReimbursementAmountInvalid: Boolean,
) {
    ul {
        for (reimbursement in reimbursements) {
            li {
                +"${usernameOf(reimbursement.fromUser)} reimbursed ${usernameOf(reimbursement.toUser)}: ${formatEuros(reimbursement.amount)}"
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
                    +"${usernameOf(suggestion.fromUser)} → ${usernameOf(suggestion.toUser)}: ${formatEuros(suggestion.amount)}"
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
                                +usernameOf(member)
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

                    if (newReimbursementAmount != null)
                        value = newReimbursementAmount
                }
            }

            if (newReimbursementAmountInvalid)
                +" Invalid reimbursement"
        }

        input(InputType.submit) {
            value = "Reimburse"
        }
    }
}
