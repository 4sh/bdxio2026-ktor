# Mission : typage et argent

Bienvenue !

Dans cette mission, nous allons explorer comment le compilateur Kotlin peut être utilisé pour éviter de commettre des erreurs.

## Situation

Nous nous intéressons au fichier [`ProjectService.kt`](../app/src/main/kotlin/services/ProjectService.kt).

Dans les classes `Transaction`, `Reimbursement`, `AmountPerUser` et `SuggestedReimbursement`, on retrouve des propriétés `amount` ou `balance` de type `Long`.

Ces propriétés représentent des sommes d'argent. Pour éviter les erreurs d'arrondis dues à [l'utilisation des nombres à virgule flottante](https://toolcluster.app/fr/articles/why-float-sum-doesnt-match/), les sommes d'argent sont à la place stockées en _centimes d'euro_.

Puisqu'un centime euro n'est pas divisible, on peut traiter chaque somme d'argent comme un entier. `Long` est un des huit types entiers, qui stocke un entier signé sur 64 bits (≈10¹⁹).

Le problème : il n'y a aucune indication dans le code qui permet de le savoir !

## Objectif

L'objectif de cette mission est de modifier l'API publique de `ProjectService` pour remplacer `Long` (quand il est utilisé pour une somme d'argent) par un type plus explicite appelé `Euro`.

## Créer Euro

Créez un nouveau fichier Kotlin appelé `Euro.kt`, avec le contenu :

```kotlin
package fr.qsh.bdxio2026.services

class Euro(
    val inCents: Long,
)
```

Une **classe** permet de créer un type concret. Le nom d'une classe est suivi de son constructeur primaire entre parenthèses.

Le constructeur primaire contient ici un seul paramètre, `inCents`, de type `Long`. Le mot clef `val` signifie, dans un constructeur primaire, de déclarer un champ privé du même nom qui stocke la valeur du paramètre, et aussi de déclarer un getter pour ce champ.

Autrement dit, le code ci-dessus est identique au code suivant en Java :

```java
package fr.qsh.bdxio2026.services

public final class Euro {
    private final long inCents;

    public Euro(long inCents) {
        this.inCents = inCents;
    }

    public long getInCents() {
        return inCents;
    }
}
```

En revanche, une classe normale a des inconvénients : chaque instance de classe correspond à une allocation mémoire. Si on stocke une grande quantité de sommes d'argents, on gaspille de la mémoire par rapport à juste stocker des `Long`.

Ici, on ne veut pas utiliser une classe en tant que structure de données en mémoire, mais purement en temps que type distinct qui soit vérifié par le compilateur.

On va utiliser un type de classe spécial, une inline value class :
```kotlin
package fr.qsh.bdxio2026.services

@JvmInline
value class Euro(
    val inCents: Long,
)
```

Une inline value class est une classe qui est supprimée à l'exécution du programme. À chaque endroit où on utilise le type `Euro` pendant l'écriture du programme, le programme final contiendra un simple primitif, `Long`. En revanche, la classe existe bien pendant l'écriture du code, et est bien un type distinct de `Long`.

> Il s'agit ici du pattern NewType qu'on peut retrouver dans plusieurs autres langages, notamment Rust.

## Écrire un test

Dans [le dossier `src/test/kotlin`](../app/src/test/kotlin), créez un fichier `EuroTest.kt`.

Déclarez un test avec :
```kotlin
package fr.qsh.bdxio2026

import fr.qsh.bdxio2026.services.Euro
import opensavvy.prepared.runner.testballoon.preparedSuite

val EuroTest by preparedSuite {
    
    test("Create a euro") {
        check(Euro(5).inCents == 5L)
    }
    
}
```

Dans ce projet, les frameworks de test [TestBalloon](https://infix-de.github.io/testBalloon/latest/), [Prepared](https://prepared.opensavvy.dev/) et [Power Assert](https://kotlinlang.org/docs/power-assert.html) sont utilisés.

On nomme une 'suite de tests' un ensemble de tests sur le même thème. Ici, on a créé une suite appelée `EuroTest` qui contient un unique test appelé "Create a euro".

Lancez les tests :
- Dans IntelliJ : `Check`
- Dans le terminal : `./gradlew check`

> Pour pouvoir exécuter les tests individuellement dans IntelliJ, installez le plugin [TestBalloon](https://plugins.jetbrains.com/plugin/27749-testballoon).

Remplacez le premier chiffre et vérifiez que les tests ne passent plus. Est-ce que le message d'erreur du test aide à comprendre ce qui a échoué ?

## Faciliter la création

En Kotlin, on peut écrire des unités de temps avec la syntaxe `5.seconds` ou `12.milliseconds`. En suivant cet exemple, on souhaiterait pouvoir écrire `12.cents` pour créer une instance de `Euro`.

Écrivez un nouveau test qui vérifie que cette syntaxe est identique au constructeur existant :
```kotlin
test("Two equivalent syntaxes") {
    check(Euro(12) == 12.cents)
}
```

Comment faire pour que ce code compile et que le test passe ?

<details>
<summary>Indice 1</summary>

Pour regarder comment la syntaxe `5.seconds` est implémentée, écrivez le dans le corps d'un test puis utilisez `CTRL`+click sur le mot `seconds` pour naviguer sur l'implémentation.

Vous pouvez ignorer les mots-clefs suivants :
- `public` : c'est la visibilité par défaut, vous n'avez pas le spécifier
- `inline` : il s'agit d'une optimisation qui n'a pas d'importance dans notre projet
- `@kotlin.internal.InlineOnly` : il s'agit d'une optimisation qui n'est pas accessible en dehors de la bibliothèque standard

</details>
<details>
<summary>Indice 2</summary>

On cherche à créer une [extension property](https://kotlinlang.org/docs/extensions.html) sur le type `Int` (le type de `12`), dont le type est `Euro`.

</details>

## Faciliter l'affichage

Quand on souhaite afficher une somme en euros, soit dans le debugger, soit à l'utilisateur, on préfère afficher `15 €` que le nombre `1500` (sous-entendu : centimes).

En Kotlin, la fonction `toString()` est utilisée pour définir la représentation textuelle d'une classe. On écrit donc un nouveau test :
```kotlin
test("Représentation textuelle") {
    check(500.cents.toString() == "5.00\u00A0€")
}
```

Le caractère `\u00A0` est l'espace insécable.

Ajoutez d'autres exemples à votre test : zéro, un nombre de centimes de moins de 1€, un très grand nombre de centimes, un nombre négatif, etc.

Faites réussir ces tests.

<details>
<summary>Indice 1</summary>

Par défaut, une `value class` a une implémentation de `toString()` qui liste les champs. Ici, on obtient un résultat ressemblant à `Euro(inCents = 500)`, qui n'est pas le résultat attendu.

Pour obtenir le résultat attendu, il faut surcharger la fonction `toString()` pour lui donner un comportement différent. Pour se faire, allez à l'intérieur des accolades de la classe `Euro` (ajoutez-les s'il n'y en pas) et tapez `toString` suivi de ENTRÉE. L'implémentation par défaut générée par Intellij appelle `super.toString()`, qui signifie à l'implémentation par défaut.

```kotlin
@JvmInline
value class Euro(
    val inCents: Long,
) {

    override fun toString(): String {
        return super.toString()
    }
}
```

Il ne reste plus qu'à implémenter cette méthode !

</details>
<details>
<summary>Indice 2</summary>

Quand nous avons navigué dans l'application, les prix étaient affichés en euros ! Il y a donc déjà, quelque part, le code pour convertir des centimes en euro…

</details>

Si on a créé un grand nombre de cas : comment exécuter chaque cas dans un test différent (pour que les tests n'échouent pas au premier cas) ?

## Calculs de sommes

Ajoutez deux tests vérifiant des calculs arithmétiques simples :

```kotlin
test("Somme") {
    check(3.cents + 2.cents == 5.cents)
    check(57.cents + 113.cents == 170.cents)
}

test("Soustraction") {
    check(170.cents - 113.cents == 57.cents)
}

test("Double négation") {
    check(-Euro(-170) == 170.cents)
}
```

On ajoute la capacité à calculer une somme ou une soustraction d'argent, les opérateurs `+` et `-`.

Comment faire compiler et passer ces tests ?

<details>
<summary>Indice 1</summary>

On peut déclarer un opérateur arithmétique simple en écrivant :

```kotlin
operator fun Euro.plus(other: Euro): Euro {
    return // …
}
```

Chaque opérateur correspond à un nom de fonction (`+` correspond à `plus). Certains opérateurs ont des contraintes supplémentaires, qui sont documentées [ici](https://kotlinlang.org/docs/operator-overloading.html).

</details>
<details>
<summary>Indice 2</summary>

Chaque test vérifie un opérateur différent !

</details>

## Calcul de moyenne

Pour pouvoir calculer la moyenne par le nombre d'utilisateurs, il nous faut les opérateurs suivants :
```kotlin
test("Division") {
    check(5.cents / 2 == 2.cents)
    check(5.cents % 2 == 1.cents)
}
```

À noter :
- Le deuxième terme est en entier, pas une deuxième somme d'argent
- On utilise la division entière pour garantir que les résultats sont exacts

Faites compiler et passer ces tests.

## Somme d'une liste

On souhaite pouvoir calculer la somme d'une liste :

```kotlin
test("Somme d'une liste") {
    check(listOf(5.cents, 20.cents, 37.cents).sum() == 62.cents)
}
```

Faites compiler et passer ce test.

<details>
<summary>Indice 1</summary>

Il est déjà possible d'écrire :
```kotlin
listOf(5, 20, 37).sum()
```

Utilisez CTRL+click pour comprendre comment la fonction `sum()` est définie pour des entiers normaux, et inspirez-vous en pour écrire la fonction `sum()` sur des euros.

Vous pouvez ignorer :
- `@kotlin.jvm.JvmName` permet d'éviter les ambiguïtés de nommage sur la JVM. Vous n'allez pas en rencontrer dans cette application.

</details>

## Comparaison

On ajoute les tests suivants :
```kotlin
test("Comparaison") {
    check(57.cents > 3.cents)
    check(32.cents <= 32.cents)
    check(-1.cents > -2.cents)
    
    val unsorted = listOf(12.cents, 357.cents, -3.cents)
    val sorted = listOf(-3.cents, 12.cents, 357.cents)
    check(unsorted.sorted() == sorted)
}
```

Faites compiler et passer ce test.

_Il n'est pas nécessaire de créer une méthode `sorted()`_.

<details>
<summary>Indice 1</summary>

La méthode `sorted()` est fournie par la bibliothèque standard et est déjà capable de trier n'importe quelle liste contenant des données _triables_.

Une classe est triable si ses instances peuvent être comparées, ce qui est représenté par l'interface `Comparable`.

Il faut donc que la classe `Euro` implémente l'interface `Comparable`.

</details>

## Lecture d'une somme en euro

On ajoute le test suivant :
```kotlin
test("Lire une somme d'argent") {
    check(Euro.parse("5.67") == 567.cents)
    check(Euro.parse("5.678") == null) { "Refus de lecture, impossible d'avoir des fractions de centimes" }
    check(Euro.parse("truc") == null) { "Refus de lecture, ce n'est pas un nombre" }
}
```

Ajoutez plus de tests pour gérer les cas particuliers (négatif, zéro…).

<details>
<summary>Indice 1</summary>

Kotlin [n'a pas de mot-clef `static`](https://kotlinlang.org/docs/object-declarations.html#companion-objects).

</details>
<details>
<summary>Indice 2</summary>

Cette fonction est déjà implémentée ailleurs dans l'application !

</details>

## C'est parti !

Nous avons implémenté tous les opérateurs dont nous avons besoin. Il est l'heure du refactor !

Voici les modifications à effectuer dans `ProjectService.kt`:
- Dans la classe `SuggestedReimbursement` : remplacer `Long` par `Euro`
- En paramètre de la fonction `ProjectService.createTransaction` : remplacer `Long` par `Euro`
- En type de retour de la fonction `ProjectService.findTotalSpending` : remplacer `Long` par `Euro`
- En paramètre de la fonction `ProjectService.createReimbursement` : remplacer `Long` par `Euro`
- En paramètre de la fonction `Project.suggestReimburseements` : remplacer `Long` par `Euro`
- En paramètre de la fonction `settleBalances` : remplacer `Long` par `Euro`

Supprimer le fichier `MoneyUtils.kt`.

Pensez à adapter le reste du code, jusqu'à ce que tous les tests passent !

Quand vous avez fini, bravo ! Vous pouvez passer à [une autre mission](missions.md).
