# Mission : HTTP

Bienvenue !

Dans cette mission, nous allons ajouter des nouvelles fonctionnalités à l'application.

## Organiser les transactions

Quand les utilisateurs créent une transaction, ils choisissent uniquement un montant. Quand un projet contient un grand nombre de transactions, il devient difficile de se souvenir laquelle est laquelle.

Pour commencer, nous allons ajouter un titre à chaque transaction.

### Ajout du titre dans l'UI

Pour commencer, ajoutons le champ `title` au formulaire de création d'une transaction, qui se trouve dans le fichier [ProjectTransactions.kt](../app/src/main/kotlin/ui/pages/project/ProjectTransactions.kt).

Ce fichier contient un composant HTML généré côté serveur, utilisant la syntaxe [KotlinX.HTML](https://kotlinlang.org/docs/typesafe-html-dsl.html).
- Cette syntaxe est disponible dans une fonction d'extension sur un composant HTML, écrit en majuscule. Ici, c'est la fonction `projectTransactions` déclarée comme extension sur `DIV` : ce composant peut donc être utilisé à l'intérieur d'une `<div>`.
- Puisque le composant est une fonction, il peut prendre des paramètres pour contrôler son état.
- Pour générer un composant HTML, on utilise la fonction du même nom. Par exemple `<p>…</p>` devient `p { … }`.
- On peut utiliser `+"Du texte"` pour générer du texte.

Dans le formulaire déclaré à la fin du composant, ajoutez un champ `title` en s'inspirant du champ existant `amount`. Pensez à ajouter la gestion de l'état et la gestion du message d'erreur, comme pour le champ `amount`.

Quand vous faites une modification du code, le code est automatiquement recompilé et le serveur charge le nouveau code lors du prochain contact avec le client. Pour voir vos modifications dans le navigateur, rechargez la page, ou faites n'importe quelle action qui contacte le serveur (ajouter une transaction, changer de page…).

Dès que vous voyez le champ `title` que vous venez d'ajouter dans le formulaire, créez une nouvelle transaction avec un titre. Que se passe-t-il ?

Vérifiez avec les outils de debug de votre navigateur que le champ est bien envoyé au serveur dans le formulaire.

### Ajout du titre dans l'API

Nous avons ajouté le champ au formulaire, tel que le navigateur l'envoie au serveur. En revanche, le serveur n'en fait rien.

Quand le formulaire est validé, le navigateur exécute une requête `POST /projects/{id}/transactions`. Nous pouvons trouver le code exécuté par le serveur lors de cette requête dans le fichier [ProjectPage.kt](../app/src/main/kotlin/ui/pages/project/ProjectPage.kt).

Le serveur contient déjà une vérification que la valeur spécifiée par `amount` peut être lue comme une somme d'argent. 

Adaptez le code pour rejeter les transactions dont le titre est vide.
Par exemple, si l'utilisateur crée une transaction `  ` (deux espaces), le serveur doit refuser la création et afficher une erreur à l'utilisateur.

### Ajout du titre dans la base de données

Quand tous les champs saisis par l'utilisateur sont corrects, le serveur appelle la fonction `projects.createTransaction(…)` qui ne prend pas en compte le titre que nous avons ajouté.

Utilisez CTRL+click pour naviguer à l'intérieur de la fonction `createTransaction` et ajoutez-lui un paramètre `title` de type `String`.

Cette méthode insert un document `Transaction` dans la base de données. 

Avant de modifier le code, observons les données en base.

<details>
<summary>Méthode 1 : avec IntelliJ (nécessite la version payante)</summary>

Ouvrez l'onglet "Database" en haut à droite. Il doit déjà contenir une base de données appelée "Local".

- Si elle est vide, sélectionnez-là puis cliquez sur le bouton 'réactualiser' (icône du recyclage).
- Si elle n'apparait pas, créez-là :
  - Appuyez sur le bouton "+", choisissez MongoDB
  - Dans URL, rentrez `mongodb://localhost:27030`, puis appuyez sur "Test connection"
  - Quand le test a réussi, validez.

Étendez la base de données.

Dans MongoDB, les données sont organisées en deux niveaux :
- Les _databases_ représentent des projets. Ici, il y en a deux : `bdxio2026` (les données correspondant à vos actions) et `bdxio2026-test` (les données créées par les tests automatisés).
- Les _collections_ contiennent des _documents_ : chaque document représente une donnée de notre application. Ici, on s'intéresse à la collection `transactions`. Double-clickez dessus pour afficher ses données.

</details>
<details>
<summary>Méthode 2 : avec MongoDB Compass (gratuit)</summary>

MongoDB Compass est l'IDE officiel pour MongoDB.

Installez [MongoDB Compass](https://mongodb.com/try/download/compass).

Connectez-vous à la base de données, qui se trouve à l'URL `mongodb://localhost:27030`.

Dans MongoDB, les données sont organisées en deux niveaux :
- Les _databases_ représentent des projets. Ici, il y en a deux : `bdxio2026` (les données correspondant à vos actions) et `bdxio2026-test` (les données créées par les tests automatisés).
- Les _collections_ contiennent des _documents_ : chaque document représente une donnée de notre application. Ici, on s'intéresse à la collection `transactions`. Double-clickez dessus pour afficher ses données.

</details>
<details>
<summary>Méthode 3 : dans le terminal</summary>

En utilisant Docker, nous avons une instance de MongoDB qui tourne localement sur notre machine. L'image Docker de MongoDB contient tous les outils pour intéragir avec la base de données dans le terminal.

- Avec IntelliJ : dans l'onglet 'Services', puis dans 'Docker', 'Docker-compose: bdxio2026', 'mongo', 'bdxio2026-mongo-1' : vous pouvez voir les logs de la base de données et appuyer sur le bouton 'Terminal' pour créer un terminal à l'intérieur du conteneur de la base de données. Dans ce terminal, exécutez la commande `mongosh`.
- Sans IntelliJ : ouvrez un terminal dans le dossier [`docker`](../docker) et exécutez la commande `docker compose exec mongo /bin/mongosh`.

Dans MongoDB, les données sont organisées en deux niveaux :
- Les _databases_ représentent des projets. 
  - Utilisez `show dbs` pour lister les databases
  - Ici, il y en a deux utiles : `bdxio2026` (les données correspondant à vos actions) et `bdxio2026-test` (les données créées par les tests automatisés).
  - Utilisez `use bdxio2026` pour accéder aux données de production
- Les _collections_ contiennent des _documents_ : chaque document représente une donnée de notre application. 
  - Utilisez `show collections` pour lister les collections.
  - Ici, on s'intéresse à la collection `transactions`.
  - On utilise `db.getCollection('transactions')` pour accéder aux méthodes d'une collection.

Exécutez `db.getCollection('transactions').find()` pour afficher toutes les transactions. Elles sont affichées dans une syntaxe inspirée de JSON.

</details>

Comparez les données que vous venez de voir en base avec la classe `Transaction` située dans le fichier `ProjectService.kt`.

Nous allons ajouter un champ `title` à la classe `Transaction`. Nous ne pouvons pas le rendre obligatoire ! Puisque les données actuellement en base n'ont pas ce champ, s'il est obligatoire côté serveur, il ne sera plus possible de lire les données de la base.

Ajoutez un champ `title` à la classe `Transaction` en lui donnant une valeur par défaut en écrivant `= "Valeur par défaut"` juste après le type. Dès qu'une transaction est lue depuis la base qui n'a pas le champ `title`, la valeur par défaut sera utilisée à la place.

Pensez à mettre à jour la fonction `createTransaction` pour que le champ soit bien sauvegardé en base.

Dans l'UI, créez une nouvelle transaction en lui donnant un titre. Vérifiez en base qu'elle a bien été créée.

### Afficher le titre dans l'UI

Il ne reste plus qu'à afficher le titre des transactions dans l'UI, à l'intérieur de la liste `ul {}` générée dans le composant `projectTransactions`.

Créez une transaction avec un titre et vérifiez que son titre est bien affiché dans l'UI.

Si c'est le cas, bravo ! Vous avez ajouté votre première fonctionnalité.

Pour la prochaine, vous serez moins guidés… Arriverez-vous à relever le défi ?

***

## Changement de mot de passe

Actuellement, quand un utilisateur crée son compte, il choisit un mot de passe, mais il n'est pas possible de le modifier.

Votre mission est d'ajouter une nouvelle page de modification de mot de passe, accessible depuis la page d'accueil pour un utilisateur connecté.

Dans cette page, l'utilisateur doit spécifier son mot de passe existant, un nouveau mot de passe, et la confirmation de ce nouveau mot de passe.

Si le mot de passe existant correspond à son mot de passe actuel, et que le nouveau mot de passe et la confirmation sont identiques, alors, **en une seule requête à la base données** (pour garantir l'atomicité et garantir qu'un utilisateur ne peut pas se retrouver sans mot de passe valide) :
- Changer le hash, le salt et le nombre d'itérations du mot de passe par ceux correspondant au nouveau mot de passe.
- Invalider tous les tokens d'authentification à part celui utilisé actuellement (= déconnexion forcée à toutes les autres sessions de cet utilisateur).

Bonne chance !

<details>
<summary>Aide : UI & cookies</summary>

Inspirez-vous des [pages existantes](../app/src/main/kotlin/ui/pages/auth) en rapport avec la gestion des droits.

</details>
<details>
<summary>Aide : Validation du mot de passe</summary>

Inspirez-vous des méthodes existantes dans [`UserService.kt`](../app/src/main/kotlin/services/UserService.kt) pour comprendre comment hasher des mots de passe et comment vérifier un mot de passe haché.

Il peut être intéressant de créer des nouvelles fonctions utilitaires pour réutiliser du code déjà utilisé dans d'autres fonctions.

</details>
<details>
<summary>Aide : Base de données</summary>

Vous pouvez consulter la [liste des opérateurs d'update](https://ktmongo.opensavvy.dev/api/dsl/opensavvy.ktmongo.dsl.query/-update-query/index.html). De quels opérateurs avez-vous besoin ?

</details>

Pour vérifier le comportement de votre projet :

- Ouvrez l'application dans un navigateur
- Ouvrez l'application en navigation privée, ou dans un autre navigateur
- Dans les deux, connectez-vous avec le même utilisateur
- Dans une des deux applications, modifiez votre mot de passe
- Si vous réactualisez l'autre application, ou que vous changez de page ou faites n'importe quelle action, vous devriez être redirigé vers la page de connexion
- Sur cette même page, connectez-vous avec le nouveau mot de passe

Si c'est réussi, bravo, vous avez écrit une fonctionnalité allant de l'UI jusqu'à la base de données ! Vous pouvez passer à [une autre mission](missions.md).
