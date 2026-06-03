# Reconnaissance de visages par ACP

Projet Java de reconnaissance de visages basé sur l'ACP, ou Analyse en Composantes Principales. L'objectif est de transformer des images de visages en vecteurs, de réduire leur dimension avec des eigenfaces, puis d'identifier la personne la plus proche dans une base d'apprentissage.

Ce projet a été réalisé dans un contexte académique à CY Tech. Il met l'accent sur la compréhension complète du pipeline : préparation des images, algèbre linéaire, réduction de dimension, projection dans un espace de caractéristiques, recherche de similarité et évaluation des résultats.

## Sommaire

- [Objectifs](#objectifs)
- [Principe général](#principe-général)
- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Dataset](#dataset)
- [Installation](#installation)
- [Utilisation](#utilisation)
- [Résultats obtenus](#résultats-obtenus)
- [Structure du dépôt](#structure-du-dépôt)
- [Limites et améliorations possibles](#limites-et-améliorations-possibles)

## Objectifs

- Implémenter une chaîne de reconnaissance faciale avec ACP.
- Réduire des images 64x64, soit 4096 pixels, vers un espace de dimension plus faible.
- Construire une base de signatures par individu.
- Comparer une image inconnue aux signatures connues avec une distance cosinus.
- Gérer le cas open set : une image peut appartenir à une personne absente de la base d'entraînement.
- Évaluer le modèle avec des métriques simples et lisibles.

## Principe général

Chaque image est convertie en niveaux de gris puis vectorisée. Le modèle calcule ensuite une base ACP à partir des images d'entraînement :

1. Chargement des images depuis un dossier structuré par identités.
2. Transformation de chaque image en vecteur de pixels.
3. Standardisation des pixels à partir des statistiques du train.
4. Centrage des images autour du visage moyen.
5. Calcul de la matrice de covariance.
6. Extraction itérative des axes propres dominants.
7. Construction des eigenfaces.
8. Projection des images dans l'espace ACP.
9. Recherche de la meilleure correspondance avec une distance cosinus.
10. Application d'un seuil personnalisé par personne pour accepter ou rejeter une prédiction.

Le modèle ne se contente donc pas de choisir le voisin le plus proche : il peut aussi retourner une absence de correspondance lorsque l'image ne semble appartenir à aucune identité connue.

## Fonctionnalités

- Chargement automatique d'images `.jpg` et `.png`.
- Support d'images déjà préparées en 64x64 niveaux de gris.
- Recadrage et redimensionnement automatique pour certaines images CelebA au format brut 178x218.
- Calcul du visage moyen.
- Calcul des eigenfaces par ACP.
- Réduction de dimension avec conservation d'inertie.
- Suppression des premiers axes dominants lors de la comparaison afin de limiter l'effet de variations globales.
- Construction d'une base de signatures `label -> vecteurs projetés`.
- Recherche du meilleur match par distance cosinus.
- Seuil adaptatif par individu, calculé à partir de la variabilité interne de ses images.
- Évaluation avec personnes connues et inconnues.
- Génération optionnelle d'images de debug : visage moyen, visages centrés et eigenfaces.

## Architecture

Le coeur du projet se trouve dans le module Maven `maths`.

| Classe | Rôle |
| --- | --- |
| `abstraction.Image` | Représente une image, lit les pixels, vectorise, convertit et sauvegarde des images préparées. |
| `abstraction.ImageLoader` | Charge un dossier d'images organisé par personne. |
| `abstraction.PCA` | Calcule l'ACP, les eigenfaces, les projections et la base de signatures. |
| `abstraction.Query` | Compare une image projetée à la base de signatures avec distance cosinus et seuils personnalisés. |
| `abstraction.Evaluator` | Lance une évaluation complète sur train/test et affiche les métriques. |
| `math.*` | Bibliothèque mathématique embarquée utilisée pour les vecteurs, matrices et opérations d'algèbre linéaire. |

Un diagramme de classes est disponible ici :

![Diagramme de classes](diagramme/diagramme_classe.png)

## Dataset

Le projet utilise des visages issus du dataset AT&T/Olivetti.

Contenu présent dans le dépôt :

- `olivetti_faces.mat` : fichier source du dataset.
- `data_filtred3/train` : images d'entraînement.
- `data_filtred3/test` : images de test.
- `data_filtred3/manifest.txt` : description du split attendu.

Le split actuellement présent contient :

| Ensemble | Identités visibles | Images |
| --- | ---: | ---: |
| Train | 39 | 273 |
| Test | 39 | 117 |

Chaque image est au format JPEG en niveaux de gris, avec une taille de 64x64 pixels. Le dossier est organisé par identité :

```text
data_filtred3/
├── train/
│   ├── 0/
│   │   ├── img_01.jpg
│   │   └── ...
│   └── ...
└── test/
    ├── 0/
    │   ├── img_01.jpg
    │   └── ...
    └── ...
```

## Installation

### Prérequis

- Java 21
- Maven 3

Vérifier les versions :

```bash
java --version
mvn --version
```

### Compiler le projet

Depuis la racine du dépôt :

```bash
mvn -q -pl maths compile
```

La compilation génère les classes Java dans :

```text
maths/target/classes
```

## Utilisation

### Lancer l'évaluation principale

```bash
java -cp maths/target/classes abstraction.Evaluator
```

Cette commande :

- entraîne l'ACP sur `data_filtred3/train` ;
- utilise au maximum 20 identités pour construire la base connue ;
- teste les images de `data_filtred3/test` ;
- sépare les cas connus et inconnus ;
- affiche les métriques finales.

### Exemple de sortie

```text
Evaluation terminee : 117 images testees depuis data_filtred3/test
trainingIndividuals=20
knownEvaluated=60
knownCorrect=49
knownRejected=6
knownMisclassified=5
unknownEvaluated=57
unknownAccepted=38
unknownRejected=19
accuracy=81.67%
discriminationRate=95.00%
openSetAccuracy=58.12%
```

### Lecture des métriques

- `accuracy` : proportion d'images connues correctement reconnues.
- `discriminationRate` : capacité du plus proche voisin à retrouver la bonne identité, sans tenir compte du seuil d'acceptation.
- `openSetAccuracy` : score global qui compte les identités connues bien reconnues et les identités inconnues correctement rejetées.
- `knownRejected` : images connues rejetées par le seuil.
- `unknownAccepted` : images inconnues acceptées à tort comme une identité connue.

## Résultats obtenus

Sur l'évaluation fournie par `abstraction.Evaluator` :

| Métrique | Valeur |
| --- | ---: |
| Images testées | 117 |
| Identités utilisées pour l'entraînement | 20 |
| Images connues évaluées | 60 |
| Images connues correctement reconnues | 49 |
| Images connues rejetées | 6 |
| Images connues mal classées | 5 |
| Images inconnues évaluées | 57 |
| Images inconnues correctement rejetées | 19 |
| Accuracy sur identités connues | 81.67% |
| Taux de discrimination | 95.00% |
| Accuracy open set | 58.12% |

Ces résultats montrent que l'espace ACP discrimine bien les identités connues, mais que le rejet des personnes inconnues reste perfectible. C'est un point intéressant du projet : il ne s'agit pas seulement de reconnaître, mais aussi de savoir quand ne pas reconnaître.

## Structure du dépôt

```text
.
├── README.md
├── pom.xml
├── olivetti_faces.mat
├── data_filtred3/
│   ├── manifest.txt
│   ├── train/
│   └── test/
├── diagramme/
│   ├── diagramme_classe.png
│   └── test.pdf
├── maths/
│   ├── pom.xml
│   └── src/
│       ├── abstraction/
│       └── math/
├── test/
└── testImg/
```

## Détails techniques

### Représentation des images

Une image 64x64 est représentée par un vecteur de 4096 valeurs. Chaque valeur correspond à l'intensité moyenne du pixel en niveaux de gris.

### ACP et eigenfaces

L'ACP permet de créer une nouvelle base où les axes capturent les variations principales des visages. Les vecteurs propres transformés dans l'espace image deviennent les eigenfaces. Chaque visage peut ensuite être représenté par quelques coordonnées au lieu de 4096 pixels.

### Similarité

La comparaison utilise une distance cosinus :

```text
distance(a, b) = 1 - cos(a, b)
```

Plus la distance est faible, plus les deux signatures sont proches.

### Seuil par individu

Pour chaque identité, le projet calcule un seuil à partir des distances internes entre ses images d'entraînement. Cela permet d'adapter la tolérance à la variabilité de chaque personne.

## Vérification

Commande exécutée avec succès :

```bash
mvn -q -pl maths compile
```

Commande d'évaluation exécutée avec succès :

```bash
java -cp maths/target/classes abstraction.Evaluator
```

Note : certaines classes de tests manuels historiques utilisent encore d'anciens chemins comme `data_filtred/train`. L'évaluation principale documentée ci-dessus utilise bien `data_filtred3/train` et `data_filtred3/test`.

## Limites et améliorations possibles

- Ajuster les seuils pour réduire le nombre d'inconnus acceptés à tort.
- Remplacer certains tests manuels par de vrais tests unitaires Maven/JUnit.
- Ajouter une interface graphique ou une interface en ligne de commande pour tester une image donnée.
- Sauvegarder et recharger proprement le modèle ACP entraîné.
- Comparer l'ACP avec d'autres méthodes : LDA, k-NN sur embeddings, SVM ou réseaux de neurones.
- Nettoyer les anciens chemins de test pour uniformiser tous les scripts sur `data_filtred3`.
- Ajouter une analyse plus détaillée des erreurs de classification.


## Licence

Aucune licence n'est actuellement définie dans le dépôt. Avant une publication publique, il est recommandé d'ajouter une licence adaptée et de vérifier les conditions d'utilisation du dataset.
