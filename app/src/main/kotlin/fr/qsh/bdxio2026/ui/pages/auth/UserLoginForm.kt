package fr.qsh.bdxio2026.ui.pages.auth

import kotlinx.html.*

fun FORM.userLoginForm(
    /**
     * The username entered by the user on their previous navigation of the form. `null` if first visit.
     */
    username: String?,

    /**
     * `true` to display an error stating the credentials are invalid.
     */
    invalidCredentials: Boolean,
) {
    p {
        label {
            +"Username: "
            input(InputType.text, name = "username") {
                required = true
                autoFocus = true
                autoComplete = "username"

                if (username != null)
                    value = username
            }
        }
    }

    p {
        label {
            +"Password: "
            input(InputType.password, name = "password") {
                required = true
                autoComplete = "current-password"
            }
        }

        if (invalidCredentials)
            +" Invalid username or password"
    }

}
