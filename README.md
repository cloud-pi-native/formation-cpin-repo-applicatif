# Application d'exemple pour déploiement sur Cloud Pi Native

Vous en êtes à l'étape 2 de la formation CPiN :
1. [Gestion des projets CPiN](https://github.com/cloud-pi-native/formation-cpin-gestion-projet)
2. ➡️ [Application d'exemple pour déploiement sur CPiN](https://github.com/cloud-pi-native/formation-cpin-repo-applicatif)
3. [Gestion des artefacts sur CPiN](https://github.com/cloud-pi-native/formation-cpin-harbor-trivy)
4. [Chart Helm de démonstration sur CPiN](https://github.com/cloud-pi-native/formation-cpin-deploiement)
5. [Gestion des secrets sur CPIN](https://github.com/cloud-pi-native/formation-cpin-gestion-secret)
6. [Observabilité sur CPiN](https://github.com/cloud-pi-native/formation-cpin-observabilite)

## Description de l'application

Cette application est un backend Java listant des données en base de données.
Les données présentes dans le fichier [demo.csv](src/main/resources/db/data/demo.csv) sont automatiquement chargées 
lors du premier démarrage de l'application :
```csv
id;name
1;Alice
2;Bob
3;Charles
4;Denis
5;Emily
```

### Processus de compilation et construction de l'application

Le concept de "multistage build" dans Docker permet de définir plusieurs étapes de construction dans un même Dockerfile.
Chaque étape utilise une image de base différente et peut exécuter des commandes spécifiques.

L'intérêt principal est de séparer la phase de compilation (qui nécessite souvent de nombreux outils et dépendances) 
de la phase d'exécution (qui n'a besoin que de l'artefact final, comme un fichier .jar). 

Ainsi, l'image finale est plus légère, plus sécurisée et ne contient que ce qui est strictement nécessaire pour faire 
tourner l'application. Dans l'exemple ci-dessous, la première étape utilise une image Maven pour compiler le projet, 
puis la seconde étape ne récupère que le .jar généré dans une image Java minimaliste, sans outils de build.


```Dockerfile
# First stage: complete build environment
FROM maven:3.9.7-eclipse-temurin-21 AS builder

# add pom.xml and source code
ADD ./pom.xml pom.xml
ADD ./src src/
RUN mvn clean package -Dmaven.test.skip=true

FROM gcr.io/distroless/java21:nonroot
WORKDIR /app
COPY --from=builder target/*.jar /app/app.jar

CMD ["-jar", "/app/app.jar"]
EXPOSE 8080
```

La construction de l'image applicative s'effectue donc par les étapes suivantes :
1. Construction de l'image Docker via la commande `docker build`
2. Envoi de l'image construire dans le référentiel d'image via la commande `docker push`

## Intégration à la chaine CPiN

### Ajout du dépôt externe

Ajoutez le dépôt de code de cette application à l'offre Cloud Pi Native de votre projet.

▶️ Depuis votre *projet*, allez dans l'onglet `Dépôt`, puis `+ Ajouter un nouveau dépôt` :

![depot](./img/depots.png)

▶️ Ajoutez un dépôt avec les paramètres suivants :
- `Nom du dépôt Git interne` : app-java
- `Url du dépôt Git externe` : https://github.com/cloud-pi-native/formation-cpin-repo-applicatif.git

▶️ Le dépôt est public et ne contient pas de code d'infrastructure. Laissez les autres cases décochées.

▶️ Cliquez sur le bouton `Ajouter le dépôt` et attendre que le dépôt apparaisse dans la console.

▶️ Depuis l'onglet `Services externes`, vérifiez en cliquant sur le service Gitlab que le dépôt *app-java* est bien 
présent dans vos projets gitlab.

![depot](./img/services-externes.png)

### Gitlab

Lors de l'accès à Gitlab à travers la console CPiN, celle-ci ouvre un nouvel onglet directement sur le groupe Gitlab 
correspondant à votre projet. Ce groupe contient différents dépôts :
- les **dépôts de code et d'infrastructure** que vous aurez déclaré dans la console (normalement, vous devriez avoir le 
dépôt **app-java** que vous venez d'ajouter)
- **infra-observability** : Ce dépôt est créé automatiquement par la Console CPiN et doit contenir les "dashboards as 
code" (plus d'information dans la documentation [https://cloud-pi-native.fr/agreement/observability#dashboard-as-code](https://cloud-pi-native.fr/agreement/observability#dashboard-as-code))
- **mirror** : Ce dépôt est créé automatiquement par la Console CPiN. Il contient un job Gitlab-ci permettant de 
synchroniser automatiquement les dépôts externes que vous ajoutez dans la console vers les dépôts internes sur Gitlab
- **infra-apps** : Ce dépôt est créé automatiquement par la Console CPiN et est lié à une feature qui n'est pas encore
implémentée. Il n'est pas utilisé actuellement.

Exemple :

![repos](./img/repo-gitlab.jpg)

### Synchronisation des dépôts

Les jobs de synchronisation peuvent se voir depuis le dépôt mirror puis dans le menu `Build`>`Pipelines` :

![jobs](./img/job-synchro-mirror.png)

L'exécution de ce job peut se faire de plusieurs manières :

#### Depuis la console CPiN

Depuis la console CPiN, cliquez sur votre dépôt puis sur le bouton `Lancer la synchronisation`. 
À noter que lors de l'ajout d'un dépôt, une première synchronisation de toutes les branches est effectuée par la console.

 ![synchro depuis la console](./img/synchro-console.png)

#### Depuis Gitlab

Dans le projet mirror, allez dans `Build`>`Pipelines` puis cliquez sur le bouton `New Pipeline`. 
Dans le formulaire de lancement, faites attention à la branche que vous choisissez de synchroniser. Pensez également à 
renseigner le champ ***PROJECT_NAME*** en spécifiant le nom du projet cible (*app-java* par exemple).

![synchro depuis gitlab](./img/job-synchro-gitlab.png)

### Ajout du fichier gitlab-ci

Gitlab est configurée pour utiliser un fichier nommé `.gitlab-ci-dso.yml` qui doit être présent à la racine du projet.
Cette convention vous permet de ne pas écraser un éventuel fichier de CI déjà utilisé par votre dépôt externe.

> [!TIP] Pour des raisons de facilité pour la suite du tutorial, nous allons travailler directement à partir du dépôt de
> code interne dans Gitlab. En conditions de travail normales, il conviendrait de travailler depuis le dépôt externe et 
> de procéder à des synchronisations de ce dépôt externe vers le dépôt interne hébergé sur Gitlab.

▶️ Depuis Gitlab, allez dans le projet `app-java` et vérifiez que vous êtes bien à la racine du projet et sur la branche
`tuto` en haut à gauche. Ensuite, cliquez sur le bouton `+`>`New file`. Appelez votre fichier `.gitlab-ci-dso.yml`.

> [!IMPORTANT] Attention à bien nommer le fichier **exactement** `.gitlab-ci-dso.yml`, faute de quoi il ne serait pas 
> pris en compte.

▶️ Ajoutez la première partie suivante :

```yaml
include:
  - project: $CATALOG_PATH
    file:
      - vault-ci.yml
      - kaniko-ci.yml
    ref: main
  - local: "/includes/java-mvn.yml"
```

Cette partie permet de charger les tâches pré-définies et pré-paramétrées pour s'exécuter dans CPiN. 
Pour plus d'information sur le catalogue, voir le repo [dédié](https://github.com/cloud-pi-native/gitlab-ci-catalog)

▶️ Ajoutez à ce même fichier la partie suivante, qui permet de définir les valeurs à mettre en cache, les variables et 
les étapes de construction :

```yaml
cache:
  paths:
    - .m2/repository/
    - node_modules

variables:
  TAG: "${CI_COMMIT_REF_SLUG}"
  DOCKERFILE: Dockerfile
  REGISTRY_URL: "${IMAGE_REPOSITORY}"

stages:
  - read-secret
  - test-app
  - docker-build
  ```

La construction du projet se fait en plusieurs étapes :
1. Lecture des secrets du projet (token gitlab, Nexus, Sonarqube, etc.) par la tache vault-ci (importée via la section 
*include* ci-dessus)
2. Exécution des tests unitaires
3. Construction de l'image docker et push vers Harbor par la tache kaniko-ci (importée via la section *include* 
ci-dessus)

#### Lecture des secrets
▶️ Ajoutez toujours dans le fichier `.gitlab-ci-dso.yml`, le bloc suivant pour lire les secrets du projet depuis Vault :

```yaml
read_secret:
  stage: read-secret
  extends:
    - .vault:read_secret
```

#### Qualimétrie de l'application
▶️ Ajoutez la description de l'étape de tests et de qualimétrie (Sonarqube) au fichier :

```yaml
test-app:
  variables:
    BUILD_IMAGE_NAME: maven:3.8-openjdk-17
    WORKING_DIR: .
  stage: test-app
  extends:
    - .java:sonar
  allow_failure: true
```

Cette partie permet notamment de créer le projet sur l'instance SonarQube CPiN.

#### Construction de l'image et déploiement sur Harbor

▶️ Ajoutez enfin le bloc suivant pour construire et déployer l'image Docker sur Harbor :

```yaml
docker-build:
  variables:
    WORKING_DIR: "."
    IMAGE_NAME: java-demo
  stage: docker-build
  extends:
    - .kaniko:simple-build-push
```

Pour information, le bloc ci-dessus est une extension (mot-clef *extends*) d'une tache issue du dépôt 
[catalogue de pipelines Gitlab](https://github.com/cloud-pi-native/gitlab-ci-catalog?tab=readme-ov-file#simple-build-push) 
de la CiPN. Le détail de cette tâche est le suivant :

> [!WARNING] Attention, vous n'avez pas besoin d'ajouter ce bloc à votre fichier.

```yaml
.kaniko:simple-build-push:
  variables:
    DOCKERFILE: Dockerfile
    WORKING_DIR: .
    IMAGE_NAME: $IMAGE_NAMES
    EXTRA_BUILD_ARGS: ""
  image:
    name: gcr.io/kaniko-project/executor:debug
    entrypoint: [""]
  script:
    # CA
    - if [ ! -z $CA_BUNDLE ]; then cat $CA_BUNDLE >> /kaniko/ssl/certs/additional-ca-cert-bundle.crt; fi
    - mkdir -p /kaniko/.docker
    - echo "$DOCKER_AUTH" > /kaniko/.docker/config.json
    - /kaniko/executor --build-arg http_proxy=$http_proxy --build-arg https_proxy=$https_proxy --build-arg no_proxy=$no_proxy $EXTRA_BUILD_ARGS --context="$CI_PROJECT_DIR" --dockerfile="$CI_PROJECT_DIR/$WORKING_DIR/$DOCKERFILE" --destination $REGISTRY_URL/$IMAGE_NAME:$TAG
```

### Fichier `.gitlab-ci-dso.yml` complet

Une fois complet, le contenu de votre fichier *YAML* sera donc le suivant :

```yaml
include:
  - project: $CATALOG_PATH
    file:
      - vault-ci.yml
      - kaniko-ci.yml
    ref: main
  - local: "/includes/java-mvn.yml"

cache:
  paths:
    - .m2/repository/
    - node_modules

variables:
  TAG: "${CI_COMMIT_REF_SLUG}"
  DOCKERFILE: Dockerfile
  REGISTRY_URL: "${IMAGE_REPOSITORY}"

stages:
  - read-secret
  - test-app
  - docker-build

read_secret:
  stage: read-secret
  extends:
    - .vault:read_secret

test-app:
  variables:
    BUILD_IMAGE_NAME: maven:3.8-openjdk-17
    WORKING_DIR: .
  stage: test-app
  extends:
    - .java:sonar
  allow_failure: true

docker-build:
  variables:
    WORKING_DIR: "."
    IMAGE_NAME: java-demo
  stage: docker-build
  extends:
    - .kaniko:simple-build-push
```

## Exécution de la chaine CI par gitlab

▶️ Une fois que le fichier est créé, *commit* puis *push* sur le dépôt Gitlab, sélectionnez dans le menu de gauche
`Build`>`Pipelines` et cliquez sur le bouton `New pipeline`. Vérifiez que vous exécutez bien votre pipeline sur la bonne
branche et lancez votre pipeline.

Le pipeline cherche automatiquement le fichier `.gitlab-dso.yaml` à la racine du projet et exécute le pipeline.

![build](img/build.png)

Le build s'est bien passé si les 3 étapes sont vertes. Il est possible de consulter les logs associés à chacune des 
étapes en cliquant sur la coche verte (ou sur l'erreur si le build est KO).

Les logs d'un build OK sur la troisième étape de build se terminent par une étape de push de l'image docker sur Harbor 
avant le log final `Job succeeded` :

![logs](img/logs-build-ok.png)

## SonarQube

Une fois le projet construit, il est possible de consulter le rapport SonarQube.

▶️ Pour cela, depuis la console CPiN, allez dans `Services externes` puis cliquez sur la tuile `SonarQube` : 

![acces sonar](img/console-tuile-sonar.png)

▶️ Depuis l'onglet *projects* de *SonarQube* sur lequel vous arrivez normalement par défaut, cliquez sur le projet 
java et choisissez la branche tuto en haut à gauche pour afficher les informations du projet : 

![sonar projet](img/sonarqube-projet.png)

Bravo, vous avez terminé le tutoriel sur la partie construction d'application !