# Boutique API

API REST de gestion de commandes pour une boutique en ligne. Projet support
pour le TP "Optimisation de la qualite des developpements logiciels".


## Contexte fonctionnel

L'application permet de gerer :

- des **clients**, de type `STANDARD` ou `PREMIUM` ;
- un **catalogue de produits**, avec prix unitaire et stock disponible ;
- des **commandes**, composees de lignes de commande, avec calcul du montant
  brut, d'une remise, des frais de livraison et du montant final.

Une commande suit un cycle de vie a travers les statuts suivants :

```
CREATED -> PAID -> SHIPPED
CREATED -> PAID -> CANCELLED
CREATED -> CANCELLED
```

## Architecture generale

Projet Spring Boot mono-module, organise en couches classiques :

```
src/main/java/com/formation/qualite/boutique/
  controller/   endpoints REST
  service/      logique metier
  repository/   acces aux donnees (Spring Data JPA)
  model/        entites JPA et enumerations
  dto/          objets de requete pour l'API
  config/       initialisation de donnees de demonstration
```

La persistance repose sur une base **H2 en memoire**, reinitialisee a chaque
demarrage avec un jeu de donnees de demonstration (clients et produits).

## Prerequis

- Java 21
- Maven 3.9+ (ou le wrapper Maven si ajoute au projet)

## Lancement de l'application

```bash
mvn spring-boot:run
```

L'application demarre sur `http://localhost:8080`.

La console H2 est disponible sur `http://localhost:8080/h2-console`
(JDBC URL : `jdbc:h2:mem:boutiquedb`, utilisateur `sa`, mot de passe vide).

## Commandes Maven

| Commande | Effet |
|---|---|
| `mvn spring-boot:run` | Demarre l'application |
| `mvn test` | Execute les tests unitaires et genere le rapport de couverture JaCoCo (`target/site/jacoco/index.html`) |
| `mvn clean verify` | Compile, teste et construit le projet, puis verifie le lint (Checkstyle) et le seuil de couverture (80 %) |

## Analyse de qualite (SonarQube)

Le TP s'appuie sur un serveur SonarQube mutualise. Voir
[README-SONAR.md](README-SONAR.md) pour la marche a suivre (connexion,
lancement d'une analyse, lecture des resultats).

## Pipeline CI

Le workflow [.github/workflows/ci.yml](.github/workflows/ci.yml) enchaine,
a chaque push sur une branche de groupe :

```
Checkout -> Build -> Lint -> Tests -> Couverture -> Analyse statique -> Quality Gate -> Packaging
```

| Etape | Outil | Effet en cas d'echec |
|---|---|---|
| Build | `mvn test-compile` | Bloquant |
| Lint | Checkstyle (`config/checkstyle/checkstyle.xml`) | Bloquant |
| Tests | JUnit, `mvn test` | Bloquant |
| Couverture | JaCoCo, seuil de 80 % des lignes et des branches | Bloquant |
| Analyse statique | SonarQube (push uniquement) | Bloquant si l'analyse ne peut pas s'executer |
| Quality Gate | Quality Gate du projet SonarQube | Bloquant |
| Packaging | `mvn package`, jar publie en artefact | Bloquant |
| Rapports | Tests, couverture et lint publies en artefact | Informatif |

Prerequis : un secret de depot `SONAR_TOKEN` contenant le jeton d'analyse
du groupe. La branche `groupeN` est analysee dans le projet `tpfilrouge-gN`.

## Endpoints disponibles

### Clients

| Methode | URL | Description |
|---|---|---|
| GET | `/api/customers` | Liste des clients |
| GET | `/api/customers/{id}` | Detail d'un client |
| POST | `/api/customers` | Creation d'un client |

### Produits

| Methode | URL | Description |
|---|---|---|
| GET | `/api/products` | Liste des produits |
| GET | `/api/products/{id}` | Detail d'un produit |
| POST | `/api/products` | Creation d'un produit |

### Commandes

| Methode | URL | Description |
|---|---|---|
| POST | `/api/orders` | Creation d'une commande |
| GET | `/api/orders` | Liste des commandes |
| GET | `/api/orders/{id}` | Detail d'une commande |
| POST | `/api/orders/{id}/pay` | Passage au statut `PAID` |
| POST | `/api/orders/{id}/ship` | Passage au statut `SHIPPED` |
| POST | `/api/orders/{id}/cancel` | Passage au statut `CANCELLED` |
| GET | `/api/orders/{id}/invoice` | Apercu texte de la facture |

## Exemples curl

Lister les clients de demonstration :

```bash
curl http://localhost:8080/api/customers
```

Lister les produits :

```bash
curl http://localhost:8080/api/products
```

Creer une commande (adapter les identifiants a ceux renvoyes par les
endpoints ci-dessus) :

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
        "customerId": 1,
        "lines": [
          { "productId": 1, "quantity": 2 },
          { "productId": 3, "quantity": 1 }
        ]
      }'
```

Faire progresser une commande :

```bash
curl -X POST http://localhost:8080/api/orders/1/pay
curl -X POST http://localhost:8080/api/orders/1/ship
```

## Regles metier connues

1. Client **PREMIUM** : remise de 10 %, portee a 15 % au-dela de 1000 € de
   montant brut.
2. Client **STANDARD** : pas de remise en dessous de 1000 €, remise de 5 %
   a partir de 1000 €.
3. Livraison gratuite au-dela de 500 € (apres remise), sinon frais fixes de
   20 €.
4. Une commande annulee ne peut pas etre expediee.
5. Le stock des produits est decremente au moment de la creation de la
   commande.
