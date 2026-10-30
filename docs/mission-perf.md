# Mission : Performance

Bienvenue !

Dans cette mission, nous allons explorer la performance de nos différentes fonctionnalités.

## Situation

Nous allons analyser les requêtes exécutées par notre serveur.

<details>
<summary>Avec IntelliJ</summary>

Installez le plugin [OpenTelemetry](https://plugins.jetbrains.com/plugin/27488-opentelemetry).

Lorsque le plugin est installé, relancez le serveur avec `Run`.

Un nouvel onglet 'OpenTelemetry' est disponible. Faites quelques actions dans l'application, puis :
- Dans l'onglet 'OpenTelemetry', choisissez l'application (`app`).
- Choisissez l'onglet 'Traces' (par défaut, l'onglet 'Logs' est ouvert, qui n'est pas configuré sur ce projet).
- Choisissez une des requêtes que vous avez faites, et cliquez sur 'Examine Trace'.

</details>
<details>
<summary>Sans IntelliJ</summary>

Ajoutez un nouveau service Aspire au [docker-compose.yml](../docker/docker-compose.yml) :
```yml
  # UI: http://localhost:18888
  aspire:
      image: mcr.microsoft.com/dotnet/aspire-dashboard:latest
      ports:
        - "18888:18888"  # Dashboard UI
        - "4320:18889"   # OTLP gRPC
        - "4321:18890"   # OTLP HTTP
      environment:
        - ASPIRE_DASHBOARD_UNSECURED_ALLOW_ANONYMOUS=true
```

Relancez les conteneurs : `cd docker && docker compose up` (cette commande ne s'arrête pas).

Nous allons modifier la configuration pour que Gradle passe l'URL d'OpenTelemetry à l'application. Ouvrez [build.gradle.kts de l'application](../app/build.gradle.kts) et trouvez la tâche `run` ; ajoutez-lui les variables d'environnement d'OpenTelemetry :
```kotlin
tasks.run {
    systemProperty("io.ktor.development", "true")

    // Aspire dashboard, see docker/docker-compose.yml (UI: http://localhost:18888)
    environment("OTEL_SERVICE_NAME", "bdxio2026")
    environment("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4320")
    environment("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc")
    environment("OTEL_METRICS_EXPORTER", "otlp")
    environment("OTEL_LOGS_EXPORTER", "otlp")
    environment("OTEL_TRACES_EXPORTER", "otlp")
}
```

Relancez l'application (tuez l'application existante, puis re-lancez `./gradlew app:run`).

Faites quelques actions dans l'application, puis ouvrez [Aspire](http://127.0.0.7:18888/traces).

</details>

Nous allons nous intéresser au chargement de la page de détails d'un projet.

Dix requêtes sont exécutées vers la base de données lors du chargement de la page. Pouvez-vous retrouver le code leur correspondant à toutes ?

Vous pouvez trouver le code exécuté lors du chargement de la page dans le fichier [`ProjectPage.kt`](../app/src/main/kotlin/ui/pages/project/ProjectPage.kt), il s'agit de la requête `GET /projects/{id}`.

Parmi ces dix requêtes, est-ce qu'elles sont toutes utiles ?

Aujourd'hui, toutes les requêtes sont exécutées l'une après l'autre. Quelles sont les requêtes qui sont indépendantes et pourraient être exécutées en même temps ?

## Exécuter des requêtes en parallèle

Ce projet utilise les Coroutines, une technologie de Kotlin qui permet de facilement exécuter du code en parallèle. Les coroutines sont marquées par le mot-clef `suspend` dans la signature des fonctions.

Imaginons qu'on a le code suivant :
```kotlin
suspend fun foo() {
    val a = requeteA()
    val b = requeteB()
}
```

Si les requêtes `requeteA` et `requeteB` sont lentes et indépendantes, il est possible de les paralléliser à condition d'être dans une fonction `suspend`. Pour se faire, il faut :

1. Délimiter un 'cadre' (_scope_ en anglais) pour isoler les tâches parallèles. Le code n'est parallèle qu'à l'intérieur de ce cadre, toutes les tâches doivent avoir terminer pour pouvoir en sortir. Cela garantie qu'on n'oublie pas une tâche de fond.
2. Lancer chaque tâche avec la fonction `async`.
3. Récupérer les résultats avec la fonction `await`.

Le code devient donc :
```kotlin
suspend fun foo() {
    coroutineScope {
        val aAsync = async { requeteA() }
        val bAsync = async { requeteB() }
        
        val a = aAsync.await()
        val b = bAsync.await()
    }
}
```

Utilisez cette technique pour paralélliser des requêtes faites par l'application. Quel gain de performance obtenez-vous ?

## Éviter des requêtes inutiles

Quand la même requête est exécutée plusieurs fois dans un intervalle de temps court, il peut être acceptable de réutiliser les résultats d'une exécution précédente, en supposant qu'ils n'ont pas changés. Si l'intervalle de temps est suffisamment court, ce ne sera pas visible sur le comportement de l'application.

Pour ce faire, on peut utiliser une bibliothèque de cache.

Dans le module `app`, ajoutez une dépendance sur `dev.opensavvy.pedestal:cache:3.2.2`. Inspirez-vous des dépendances existantes :
- [app/build.gradle.kts](../app/build.gradle.kts)
- [gradle/libs.versions.toml](../gradle/libs.versions.toml)

Pensez à réactualiser IntelliJ quand vous modifiez les dépendances (bouton éléphant en haut à droite d'un fichier), puis à relancer le serveur.

Trouvez où configurer du cache. Attention à ne pas introduire de failles de sécurité !

Il existe deux endroits où le cache a un impact significatif sur la performance.

Vous pouvez trouver la documentation de cette bibliothèque de cache [ici](https://pedestal.opensavvy.dev/api-docs/cache/index.html).

Quel gain de performance obtenez-vous ?

***

Au final, quel est le gain total de performance que vous avez pu obtenir ?

Vous pouvez passer à [une autre mission](missions.md).
