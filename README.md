# Application d'exemple pour déploiement sur Cloud Pi Native

Vous en êtes à l'étape 2 de la formation CPiN :

1. [Gestion des projets CPiN](https://github.com/cloud-pi-native/formation-cpin-gestion-projet)
2. ➡️ [Application d'exemple pour déploiement sur CPiN](https://github.com/cloud-pi-native/formation-cpin-repo-applicatif/tree/tuto)
3. [Gestion des artefacts sur CPiN](https://github.com/cloud-pi-native/formation-cpin-harbor-trivy)
4. [Chart Helm de démonstration sur CPiN](https://github.com/cloud-pi-native/formation-cpin-deploiement)
5. [Gestion des secrets sur CPiN](https://github.com/cloud-pi-native/formation-cpin-gestion-secret)
6. [Observabilité sur CPiN](https://github.com/cloud-pi-native/formation-cpin-observabilite)

## Description de l'application

Cette application est un backend Java listant des données en base de données.
Les données présentes dans le fichier [demo.csv](src/main/resources/db/data/demo.csv) sont automatiquement chargées lors du premier démarrage de l'application :

```csv
id;name
1;Alice
2;Bob
3;Charles
4;Denis
5;Emily
```

### Processus de compilation et construction de l'application

Le principe est de séparer la phase de compilation, qui nécessite de nombreux outils et dépendances, de la phase d'exécution, qui n'a besoin que de l'artefact final.
L'image finale est ainsi plus légère et plus sûre, puisqu'elle ne contient ni Maven, ni le code source, ni les outils de build.

Ici, la compilation est faite en amont — par la chaîne CI, ou par un `mvn clean package` en local — et le Dockerfile ne fait que déposer le .jar obtenu dans une image Java minimaliste :

```Dockerfile
FROM gcr.io/distroless/java21:nonroot
WORKDIR /app
COPY target/*.jar /app/app.jar

CMD ["-jar", "/app/app.jar"]
EXPOSE 8080
```

> [!IMPORTANT]
> L'image attend un .jar déjà présent dans `target/`. Avant un `docker build` en local, lancez donc `mvn clean package`, sinon la construction échoue faute de fichier à copier.

La construction de l'image applicative s'effectue donc par les étapes suivantes :

1. Compilation de l'application via la commande `mvn clean package`
2. Construction de l'image Docker via la commande `docker build`
3. Envoi de l'image construite dans le référentiel d'image via la commande `docker push`

> [!TIP]
> Une autre approche consiste à utiliser un *multistage build* : le Dockerfile embarque lui-même une première étape `FROM maven:3.9.7-eclipse-temurin-21 AS builder` qui compile le projet, puis une seconde étape qui récupère le .jar avec `COPY --from=builder`.
> L'image obtenue est identique et le Dockerfile devient autonome, mais l'application est alors compilée deux fois dans le pipeline : une fois pour les tests et l'analyse SonarQube, une fois pour l'image. Nous avons préféré ne la compiler qu'une seule fois.

## Intégration à la chaîne CPiN

### Ajout du dépôt externe

Ajoutez le dépôt de code de cette application à l'offre Cloud Pi Native de votre projet.

▶️ Depuis votre *projet*, allez dans l'onglet `Ressources`, puis `+ Ajouter un nouveau dépôt` :

![depot](./img/depots.png)

▶️ Ajoutez un dépôt avec les paramètres suivants :

- `Nom du dépôt Git interne` : app-java
- `Url du dépôt Git externe` : https://github.com/cloud-pi-native/formation-cpin-repo-applicatif.git

▶️ Le dépôt est public et ne contient pas de code d'infrastructure. Laissez les autres cases décochées.

▶️ Cliquez sur le bouton `Ajouter le dépôt` et attendez que le dépôt apparaisse dans la console.

▶️ Depuis l'onglet `Services externes`, vérifiez en cliquant sur le service Gitlab que le dépôt *app-java* est bien présent dans vos projets GitLab.

![service externe GitLab](./img/services-externes.png)

### Gitlab

Lors de l'accès à Gitlab à travers la console CPiN, celle-ci ouvre un nouvel onglet directement sur le groupe Gitlab correspondant à votre projet.
Ce groupe contient différents dépôts :

- les **dépôts de code et d'infrastructure** que vous aurez déclarés dans la console (normalement, vous devriez avoir le dépôt **app-java** que vous venez d'ajouter)
- **infra-observability** : Ce dépôt est créé automatiquement par la Console CPiN et doit contenir les "dashboards as code" (plus d'information dans la [documentation dashboard-as-code](https://cloud-pi-native.fr/agreement/observability#dashboard-as-code))
- **mirror** : Ce dépôt est créé automatiquement par la Console CPiN. Il contient un job Gitlab-ci permettant de synchroniser automatiquement les dépôts externes que vous ajoutez dans la console vers les dépôts internes sur Gitlab
- **infra-apps** : Ce dépôt est créé automatiquement par la Console CPiN et est lié à une feature qui n'est pas encore implémentée. Il n'est pas utilisé actuellement.

Exemple :

![repos](./img/repo-gitlab.jpg)

### Synchronisation des dépôts

Les jobs de synchronisation sont visibles depuis le dépôt `mirror` puis dans le menu `Build`>`Pipelines` :

![jobs](./img/job-synchro-mirror.png)

L'exécution de ce job peut se faire de plusieurs manières :

#### Depuis la console CPiN

Depuis la console CPiN, cliquez sur votre dépôt puis sur le bouton `Lancer la synchronisation`.
À noter que lors de l'ajout d'un dépôt, une première synchronisation de toutes les branches est effectuée par la console.

![synchro depuis la console](./img/synchro-console.png)

#### Depuis Gitlab

Dans le projet mirror, allez dans `Build`>`Pipelines` puis cliquez sur le bouton `New Pipeline`.
Dans le formulaire de lancement, faites attention à la branche que vous choisissez de synchroniser.
Pensez également à renseigner le champ ***PROJECT_NAME*** en spécifiant le nom du projet cible (*app-java* par exemple).

![synchro depuis gitlab](./img/job-synchro-gitlab.png)

### Ajout du fichier gitlab-ci-dso

Gitlab est configuré pour utiliser un fichier nommé `.gitlab-ci-dso.yml` qui doit être présent à la racine du projet.
Cette convention vous permet de ne pas écraser un éventuel fichier de CI déjà utilisé par votre dépôt externe.

> [!TIP]
> Pour des raisons de facilité pour la suite du tutoriel, nous allons travailler directement à partir du dépôt de code interne dans Gitlab.
> En conditions de travail normales, il conviendrait de travailler depuis le dépôt externe et de procéder à des synchronisations de ce dépôt externe vers le dépôt interne hébergé sur Gitlab.
> Si vous relancez une synchronisation des branches depuis la console, cela écrasera vos modifications dans le dépôt.

▶️ Depuis Gitlab, allez dans le projet `app-java` et vérifiez que vous êtes bien à la racine du projet et sur la branche `tuto` en haut à gauche.
Ensuite, cliquez sur le bouton `+`>`New file`. Appelez votre fichier `.gitlab-ci-dso.yml`.

> [!IMPORTANT]
> Attention à bien nommer le fichier **exactement** `.gitlab-ci-dso.yml`, faute de quoi il ne serait pas pris en compte.

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
Pour plus d'information sur le catalogue, voir le repo [catalogue gitlab-ci CPiN](https://github.com/cloud-pi-native/gitlab-ci-catalog)

▶️ Ajoutez à ce même fichier la partie suivante, qui permet de définir les valeurs à mettre en cache, les variables et les étapes de construction :

```yaml
workflow:
  auto_cancel:
    on_new_commit: interruptible

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

1. Lecture des secrets du projet (token GitLab, Nexus, Sonarqube, etc.) par la tâche vault-ci (importée via la section *include* ci-dessus)
2. Exécution des tests unitaires
3. Construction de l'image docker et push vers Harbor par la tâche kaniko-ci (importée via la section *include* ci-dessus)

L'application n'est compilée qu'une seule fois : le job `test-app` produit le .jar, le transmet en artefact, et le job `docker-build` se contente de l'embarquer dans l'image.

Le bloc `workflow` en tête de fichier annule automatiquement les pipelines devenus inutiles : si vous poussez un nouveau commit alors qu'un pipeline est encore en cours sur la même branche, ce dernier est interrompu au lieu de monopoliser un runner.

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
    BUILD_IMAGE_NAME: maven:3.9.7-eclipse-temurin-21
    WORKING_DIR: .
  stage: test-app
  needs:
    - read_secret
  extends:
    - .java:sonar
  artifacts:
    paths:
      - target/*.jar
    expire_in: 1 hour
  interruptible: true
```

Cette partie permet notamment de créer le projet sur l'instance SonarQube CPiN.

Le mot-clef *needs* indique que cette tâche n'attend que `read_secret`, dont elle récupère le token SonarQube.
La section *artifacts* conserve le .jar produit par la compilation : c'est lui que l'étape suivante placera dans l'image, ce qui évite de recompiler l'application.

La tâche `.java:sonar` procède en deux commandes : `mvn clean package`, qui compile et lance les tests, puis l'analyse SonarQube, qui réutilise les classes et le rapport de couverture déjà produits.
L'échec de cette seconde commande n'interrompt pas le job : si SonarQube est indisponible, un avertissement apparaît dans les logs et le pipeline continue.
Une erreur de compilation ou un test en échec, en revanche, arrêtent le pipeline : aucune image n'est alors construite.

#### Construction de l'image et déploiement sur Harbor

▶️ Ajoutez enfin le bloc suivant pour construire et déployer l'image Docker sur Harbor :

```yaml
docker-build:
  variables:
    WORKING_DIR: "."
    IMAGE_NAME: java-demo
  stage: docker-build
  needs:
    - read_secret
    - job: test-app
      artifacts: true
  extends:
    - .kaniko:simple-build-push
  interruptible: true
```

Cette tâche récupère les secrets (`DOCKER_AUTH` et `IMAGE_REPOSITORY`, lus depuis Vault) et le .jar produit par `test-app`, grâce à `artifacts: true`. L'image n'est donc construite que si la compilation et les tests ont réussi.

Pour information, le bloc ci-dessus est une extension (mot-clef *extends*) de la tâche `.kaniko:simple-build-push`, issue du [catalogue de pipelines GitLab](https://github.com/cloud-pi-native/gitlab-ci-catalog?tab=readme-ov-file#simple-build-push) de CPiN.
Elle est chargée par la section *include* : vous n'avez pas besoin de la recopier dans votre fichier.

Pour consulter son détail (variables disponibles, image kaniko utilisée, commande exécutée), voir sa définition dans le fichier [kaniko-ci.yml](https://github.com/cloud-pi-native/gitlab-ci-catalog/blob/main/kaniko-ci.yml) du catalogue.

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

workflow:
  auto_cancel:
    on_new_commit: interruptible

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
    BUILD_IMAGE_NAME: maven:3.9.7-eclipse-temurin-21
    WORKING_DIR: .
  stage: test-app
  needs:
    - read_secret
  extends:
    - .java:sonar
  artifacts:
    paths:
      - target/*.jar
    expire_in: 1 hour
  interruptible: true

docker-build:
  variables:
    WORKING_DIR: "."
    IMAGE_NAME: java-demo
  stage: docker-build
  needs:
    - read_secret
    - job: test-app
      artifacts: true
  extends:
    - .kaniko:simple-build-push
  interruptible: true
```

## Exécution de la chaîne CI par GitLab

▶️ Une fois que le fichier est créé et *commit* dans le dépôt Gitlab, sélectionnez dans le menu de gauche `Build`>`Pipelines`.
Vérifiez que le pipeline s'est déclenché automatiquement.

![build](img/build.png)

Il est possible de consulter les logs associés à chacune des étapes en cliquant sur la coche verte (ou sur l'erreur si le build est KO).

Les logs d'un build OK sur la troisième étape de build se terminent par une étape de push de l'image docker sur Harbor avant le log final `Job succeeded` :

![logs](img/logs-build-ok.png)

## SonarQube

Une fois le projet construit, il est possible de consulter le rapport SonarQube.

▶️ Pour cela, depuis la console CPiN, allez dans `Services externes` puis cliquez sur la tuile `SonarQube` :

![acces sonar](img/console-tuile-sonar.png)

▶️ Depuis l'onglet *projects* de *SonarQube* sur lequel vous arrivez normalement par défaut, cliquez sur votre projet et choisissez la branche tuto en haut à gauche pour afficher les informations du projet :

![sonar projet](img/sonarqube-projet.png)

Bravo, vous avez terminé le tutoriel sur la partie construction d'application !

Vous pouvez passer à l'étape 3 : [Gestion des artefacts sur CPiN](https://github.com/cloud-pi-native/formation-cpin-harbor-trivy)
