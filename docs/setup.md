# Configuration

## Pré-requis

Pour pouvoir suivre ce workshop, il vous faut :
- Git
- Docker
- Java 17 ou plus récent

## IDE

Pour pouvoir suivre ce workshop, il est recommandé d'utiliser un IDE comprenant le Kotlin.

Nous vous conseillons IntelliJ (la version gratuite est suffisante) :
- Installez la [JetBrains Toolbox](https://www.jetbrains.com/toolbox-app/), un petit installateur qui permet de facilement gérer les versions des outils JetBrains
- Dans la JetBrains Toolbox, choisissez IntelliJ
- Quand le téléchargement est terminé, lancez IntelliJ

## Dépôt

Dans l'écran d'accueil, choisissez "Clone repository".

> https://github.com/4sh/bdxio2026-ktor.git

Attendez que le projet finisse de s'ouvrir.

## Lancer le projet

Premièrement, lancez Docker Compose, qui va télécharger et lancer une instance locale de MongoDB.

- Via IntelliJ : `External services`
- Dans le terminal : `cd docker && docker compose up` (cette commande ne s'arrête pas)

Deuxièmement, lancez la recompilation continue. Gradle va recompiler le projet dès que le code est modifié.

- Via IntelliJ : `Auto-recompile`
- Dans un nouveau terminal : `./gradlew :app:jar --continuous` (cette commande ne s'arrête pas)

Cette commande va continuer jusqu'à ce que vous l'arrêtiez. Vous pouvez continuer dès que cette ligne est affichée :
```terminaloutput
Waiting for changes to input files... (ctrl-d to exit)
```

Troisièmement, lancez le serveur lui-même.

- Via IntelliJ : `Run`
- Dans un nouveau terminal : `./gradlew :app:run` (cette commande ne s'arrête pas)

Attendez que la ligne suivante soit affichée :
```terminaloutput
2026-10-29 21:31:12.318 [main] INFO  io.ktor.server.Application - Responding at http://0.0.0.0:8080
```

Pour finir, ouvrez la page web dans votre navigateur :

- Via IntelliJ (nécessite la version payante) : `Open website` en restant appuyé sur le bouton SHIFT
- Via un navigateur : ouvrir http://localhost:8080

Si le site s'ouvre correctement, parfait, vous avez fini la configuration du projet ! 

Prochaine étape : [découvrir l'application](spec.md).
