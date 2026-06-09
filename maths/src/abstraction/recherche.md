# Recherche d'une identité dans `Query.java`

`Query` compare l'image recherchée aux projections ACP enregistrées dans la base :

```text
label -> liste de vecteurs projetés
```

Chaque label représente une personne. La recherche repose sur la distance cosinus et
sur un seuil d'acceptation calculé séparément pour chaque personne.

## Distance utilisée

La distance réellement retournée par la méthode `distance` est la distance cosinus :

$$
d_{\cos}(a,b)
=
1-\frac{a \cdot b}{\lVert a\rVert \lVert b\rVert}
$$

Une petite distance indique donc que les deux vecteurs ont des directions proches.
Si l'un des deux vecteurs a une norme nulle, la distance retournée est `1`.

Le code calcule également une distance euclidienne normalisée dans la méthode
`distance`, mais cette valeur n'est actuellement pas utilisée dans le résultat.

## Calcul du seuil personnalisé

Pour une personne possédant les vecteurs $x_1,\ldots,x_n$, `Query` calcule d'abord
le centroïde :

$$
c=\frac{1}{n}\sum_{i=1}^{n}x_i
$$

Il calcule ensuite la distance cosinus entre chaque image et ce centroïde :

$$
d_i=d_{\cos}(x_i,c)
$$

La moyenne de ces distances est :

$$
\mu=\frac{1}{n}\sum_{i=1}^{n}d_i
$$

L'écart-type utilisé est celui de la population :

$$
\sigma=
\sqrt{\frac{1}{n}\sum_{i=1}^{n}(d_i-\mu)^2}
$$

Le seuil brut est défini par :

$$
\zeta=\mu+2\sigma
$$

Il est ensuite borné par un plancher et un plafond :

$$
\zeta_{\text{final}}
=
\min\left(0{,}30,\max\left(\zeta,\zeta_{\min}\right)\right)
$$

Avec le constructeur par défaut `new Query()`, le plancher vaut :

$$
\zeta_{\min}=0{,}01
$$

Si une personne ne possède qu'un seul vecteur, son seuil est directement égal à
$\zeta_{\min}$. Une liste absente ou vide utilise également ce seuil par défaut.

Les seuils calculés sont mémorisés dans un cache associé à chaque liste de vecteurs
afin d'éviter de les recalculer à chaque recherche.

## Sélection du meilleur résultat

Pour chaque personne $p$, la requête $q$ est comparée à toutes ses images. La
distance retenue pour cette personne est celle de son exemple le plus proche :

$$
D_p(q)=\min_{x\in p}d_{\cos}(q,x)
$$

La personne est considérée comme acceptable uniquement si :

$$
D_p(q)\leq\zeta_p
$$

Parmi toutes les personnes qui respectent leur seuil personnalisé, `findBestMatch`
retourne celle dont la distance $D_p(q)$ est la plus faible.

Si aucune personne ne passe son seuil, la méthode retourne une chaîne vide `""`,
ce qui signifie que l'image est rejetée comme inconnue.

## Diagnostic d'évaluation

La méthode `diagnoseMatch` applique le même calcul et fournit en plus :

- le label le plus proche, même s'il est rejeté par son seuil ;
- le label accepté le plus proche ;
- leurs distances ;
- la distance et le seuil du label attendu.

Cette méthode sert à distinguer une erreur de classement d'un rejet causé par le
seuil.

## Différence avec l'ancienne méthode

L'implémentation actuelle n'utilise pas la règle de Tukey, les quartiles, l'IQR,
la suppression itérative des valeurs aberrantes ni le maximum des distances.
Le seuil est uniquement fondé sur la moyenne et l'écart-type des distances au
centroïde, avec un coefficient de survariance égal à `2`.
