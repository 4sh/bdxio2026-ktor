package fr.qsh.bdxio2026.ui.pages.auth

import kotlinx.html.*

/**
 * Generates the user creation form content.
 */
fun FORM.userCreationForm(
    /**
     * The username entered by the user on their previous navigation of the form. `null` if first visit.
     */
    username: String?,

    /**
     * The password entered by the user on their previous navigation of the form. `null` if first visit.
     */
    password: String?,

    /**
     * The password confirmation entered by the user on their previous navigation of the form. `null` if first visit.
     */
    passwordConfirmation: String?,

    /**
     * `true` to display an error to the user that the [password] and [passwordConfirmation] don't match.
     */
    passwordMismatch: Boolean,

    /**
     * `true` to display an error to the user that the [username] is already taken.
     */
    usernameTaken: Boolean,
) {
    p {
        label {
            +"Username: "
            input(InputType.text, name = "username") {
                minLength = "3"
                required = true
                autoFocus = true
                autoComplete = "email"

                if (username != null)
                    value = username
            }
        }

        if (usernameTaken)
            +" Username already taken"
    }

    p {
        label {
            +"Password: "
            input(InputType.password, name = "password") {
                minLength = "6"
                required = true
                autoComplete = "new-password"

                if (password != null)
                    value = password
            }
        }
    }

    p {
        label {
            +"Confirm password: "
            input(InputType.password, name = "password-confirm") {
                minLength = "6"
                required = true
                autoComplete = "new-password"

                if (passwordConfirmation != null)
                    value = passwordConfirmation
            }
        }

        if (passwordMismatch)
            +" Passwords do not match"
    }
}
