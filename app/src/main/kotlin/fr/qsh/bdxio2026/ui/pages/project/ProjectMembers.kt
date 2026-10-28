package fr.qsh.bdxio2026.ui.pages.project

import fr.qsh.bdxio2026.services.Project
import kotlinx.html.*
import opensavvy.ktmongo.bson.types.ObjectId

/**
 * Displays the member management section of a project.
 */
fun DIV.projectMembers(
    /**
     * The project to display.
     */
    project: Project,

    /**
     * The ID of the current user.
     */
    currentUser: ObjectId,

    /**
     * If non-`null`, corresponds to the username of a new member to add to the project.
     */
    newMemberUsername: String?,

    /**
     * `true` to display an error that the entered [newMemberUsername] doesn't correspond to any user.
     */
    newMemberUnknown: Boolean,

    /**
     * Computes a user's username based on their ID.
     */
    usernameOf: (ObjectId) -> String,
) {
    h2 {
        +"Members"
    }

    ul {
        for (member in project.members) {
            li {
                +usernameOf(member)
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

                        if (newMemberUsername != null)
                            value = newMemberUsername
                    }
                }

                if (newMemberUnknown)
                    +" No such user"
            }

            input(InputType.submit) {
                value = "Add member"
            }
        }
    }
}
