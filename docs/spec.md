# Spécifications

Bonjour !

Ce document décrit les fonctionnalités de l'application que vous avez sous les yeux.

Je vous encourage à lire chaque section en réalisant les actions correspondantes dans l'application.

Si vous n'avez pas l'application sous les yeux, veuillez commencer par [la configuration du projet](setup.md).

## Introduction

Cette application permet à plusieurs utilisateurs de créer un projet commun (par exemple un voyage à la [KotlinConf](https://kotlinconf.com/), cet avril en Pologne). Chaque utilisateur note ses dépenses personnelles, et l'application suit les dépenses de chaque utilisateur pour vérifier que tous les utilisateurs ont dépensé une somme identique à la fin du voyage.

## Écran d'accueil

[Accéder à cet écran](http://localhost:8080)

Pour un utilisateur non connecté, les deux actions possibles sont de [se créer un compte](#création-de-compte), et de [se connecter](#connexion-à-un-compte).

Pour un utilisateur connecté, l'écran affiche :
- La liste des projets, qui permet de consulter les [détails d'un projet](#ouvrir-un-projet).
- Le bouton de [création de nouveau projet](#créer-un-projet).
- Le bouton de [déconnexion](#connexion-à-un-compte).

## Création de compte

L'écran de création de compte nécessite de choisir :
- Un nom d'utilisateur, de plus de 3 caractères
- Un mot de passe, de plus de 6 caractères
- Le même mot de passe, qui doit correspondre

Quand le compte a été créé avec réussite, l'utilisateur est redirigé vers la [page d'accueil](#écran-daccueil).

## Connexion à un compte

L'écran de connexion nécessite de choisir :
- Un nom d'utilisateur existant
- Le mot de passe correspondant à cet utilisateur

Quand la connexion réussie :
1. Le navigateur de l'utilisateur enregistre le cookie `token`.
2. L'utilisateur est redirigé vers la [page d'accueil](#écran-daccueil).

## Se déconnecter

Quand l'utilisateur se déconnecte :
1. Le navigateur de l'utilisateur supprime le cookie `token`.
2. L'utilisateur est redirigé vers la [page d'accueil](#écran-daccueil).

## Créer un projet

Créer un projet nécessite uniquement de lui choisir un nom.

Lorsque le projet est créé, l'utilisateur est redirigé vers la [page d'accueil](#écran-daccueil).

## Ouvrir un projet

La page de détails d'un projet est composée de quatre sections :
- [La gestion des membres](#gestion-des-membres)
- [La gestion des transactions](#gestion-des-transactions)
- [Le total des dépenses](#total-des-dépenses)
- [La gestion des remboursements](#gestion-des-remboursements)

### Gestion des membres

Un projet représente une expérience partagée entre plusieurs utilisateurs (par exemple, un voyage).

Dans cette section, le créateur du projet peut inviter d'autres utilisateurs à rejoindre le projet.

Les membres du projet peuvent consulter la liste des membres, mais non la modifier.

### Gestion des transactions

La liste des transactions affiche chaque dépense individuelle de chaque utilisateur, ainsi que leur total et la moyenne par utilisateur.

Chaque utilisateur peut déclarer une dépense que cet utilisateur a réalisée pour le groupe. Une dépense négative peut être utilisée pour régulariser une erreur.

### Total des dépenses

Cette section totalise le total que chaque utilisateur a dépensé pour le groupe.

### Gestion des remboursements

Cette section n'est affichée que si le projet contient plusieurs utilisateurs.

Chaque utilisateur peut déclarer un virement direct vers un autre utilisateur. L'application suggère quels virements les utilisateurs peuvent faire pour équilibrer les dépenses par personne.

***

Vous avez fait le tour des fonctionnalités de l'application !

Vous pouvez maintenant découvrir vos différentes [missions](missions.md).
