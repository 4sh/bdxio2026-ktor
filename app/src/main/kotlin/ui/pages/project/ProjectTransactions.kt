package fr.qsh.bdxio2026.ui.pages.project

import fr.qsh.bdxio2026.services.Project
import fr.qsh.bdxio2026.services.Transaction
import fr.qsh.bdxio2026.ui.pages.format
import fr.qsh.bdxio2026.ui.pages.formatEuros
import kotlinx.html.*
import opensavvy.ktmongo.bson.types.ObjectId

fun DIV.projectTransactions(
    /**
     * The project to display.
     */
    project: Project,

    /**
     * The transaction history.
     */
    transactions: List<Transaction>,

    /**
     * The total amount spent in all transactions.
     */
    transactionsTotal: Long,

    /**
     * The average amount spent by member.
     */
    transactionsAverage: Long,

    /**
     * Computes a user's username based on their ID.
     */
    usernameOf: (ObjectId) -> String,

    /**
     * If non-`null`, contains the amount selected by the user on their previous visit to this page.
     */
    newTransactionAmount: String?,

    /**
     * `true` to display the error that [newTransactionAmount] cannot be parsed.
     */
    newTransactionAmountInvalid: Boolean,
) {
    if (transactions.isEmpty()) {
        p {
            +"No transactions yet."
        }
    } else {
        ul {
            for (transaction in transactions) {
                li {
                    +buildString {
                        append(transaction._id.timestamp.format())
                        append(" - ")
                        append(usernameOf(transaction.userId))
                        append(": ")
                        append(formatEuros(transaction.amount))
                    }
                }
            }
        }

        p {
            +"Total: ${formatEuros(transactionsTotal)} (per user: ~${formatEuros(transactionsAverage)})"
        }
    }

    form(
        action = routeProjectTransactions(project._id),
        method = FormMethod.post,
        encType = FormEncType.applicationXWwwFormUrlEncoded
    ) {
        p {
            label {
                +"Amount (€): "
                input(InputType.number, name = "amount") {
                    required = true
                    step = "0.01"

                    if (newTransactionAmount != null)
                        value = newTransactionAmount
                }
            }

            if (newTransactionAmountInvalid)
                +" Invalid amount"
        }

        input(InputType.submit) {
            value = "Add transaction"
        }
    }
}
