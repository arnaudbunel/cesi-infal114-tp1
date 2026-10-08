# Analyse SonarQube - Guide etudiant

Ce document explique comment lancer une analyse SonarQube du projet
`boutique-api` et comment lire les resultats, dans le cadre du TP
"Optimisation de la qualite des developpements logiciels".

## 1. Serveur mis a disposition

Un serveur SonarQube Community est mis a disposition par l'enseignant,
accessible a l'adresse :

```
https://sonarqube.techsolutionsbyab.app/
```

Vous n'avez rien a installer ni a demarrer localement : pas de Docker, pas
de serveur SonarQube sur votre poste. Vous avez uniquement besoin d'une
connexion reseau vers cette URL.

## 2. Identifiants de votre groupe

Chaque groupe dispose de :

- un **project key** dedie (ex. `boutique-api-groupe3`) ;
- un **token d'analyse** personnel au groupe.

Ces deux informations vous sont communiquees en seance.
Ne les partagez pas avec les autres groupes et ne les committez jamais dans
le depot Git (ni en clair dans un fichier versionne, ni dans un message de
commit).

Vous pouvez vous connecter a l'interface web de SonarQube avec le compte
qui vous a ete fourni pour consulter le tableau de bord de votre projet.

## 3. Lancer une analyse

Depuis la racine du projet, generer d'abord les rapports de tests et de
couverture, puis lancer l'analyse :

```bash
mvn clean verify \
  org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.host.url=https://sonarqube.techsolutionsbyab.app/ \
  -Dsonar.projectKey=<project-key-de-votre-groupe> \
  -Dsonar.token=<votre-token> \
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

Remplacez `<project-key-de-votre-groupe>` et `<votre-token>` par les
valeurs qui vous ont ete communiquees.

Le plugin Sonar n'est pas declare dans le `pom.xml` du projet : il est
invoque ici directement par ses coordonnees Maven (`groupId:artifactId:goal`),
sans avoir besoin de modifier le `pom.xml` pour un premier essai.

A la fin de l'execution, le terminal affiche un lien direct vers le
tableau de bord de votre analyse sur le serveur SonarQube.

## 4. Lire les resultats

Sur le tableau de bord de votre projet, les principaux indicateurs a
regarder :

- **Quality Gate** : statut global (Passed / Failed) au regard des seuils
  configures (couverture, duplication, notes de fiabilite/securite/
  maintenabilite, etc.).
- **Bugs / Vulnerabilites / Code Smells** : problemes detectes, classes par
  severite (Blocker, Critical, Major, Minor, Info).
- **Duplications** : pourcentage de code duplique et blocs concernes
  (detection CPD, basee sur une correspondance exacte de tokens).
- **Couverture** : pourcentage de lignes/branches couvertes par les tests,
  issu du rapport JaCoCo genere lors du `mvn clean verify`.
- **Maintenabilite / Fiabilite / Securite** : notes synthetiques (A a E)
  calculees a partir de la "dette technique" estimee par l'outil (temps de
  correction estime des issues ouvertes).

Pour chaque issue remontee, SonarQube indique le fichier et la ligne
concernes, une explication du probleme, et souvent un lien vers la regle
appliquee (utile pour comprendre le "pourquoi", pas seulement le "quoi").

## 5. Ce que l'outil ne vous dira pas

SonarQube est un outil d'analyse statique : il applique des regles
generiques et ne connait pas les intentions metier du projet. Certains
problemes de conception (mauvais choix d'abstraction, couplage logique
entre classes, incoherences fonctionnelles, violations de principes SOLID
qui ne se traduisent pas par un pattern de code reconnu par une regle) ne
seront pas forcement detectes automatiquement. Une relecture humaine du
code reste necessaire en complement de l'analyse outillee.

## 6. Bonnes pratiques pour le TP

- Relancez une analyse apres chaque etape significative de refactoring
  pour mesurer l'evolution des indicateurs (nombre d'issues, duplication,
  couverture, note de maintenabilite).
- Ne cherchez pas uniquement a faire disparaitre les warnings : comprenez
  la regle declenchee avant de corriger, et documentez vos choix lorsque
  vous decidez de ne pas suivre une recommandation de l'outil.
- Comparez vos resultats d'analyse avec les observations de votre revue de
  code humaine (exercice de revue de PR) : les deux approches ne trouvent
  pas toujours les memes problemes.
