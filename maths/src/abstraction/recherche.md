L'objectif est de trouver le `seuil` le mieux et de fixer un seuil par indivdu $\zeta_{image}$.

## La méthode pour trouver le seuil $\zeta_{image}$:

On prend tous les points $p$ (de nombre $n$) dans le dataSet d'une personne, et on calcule l'épi centre
$$
    \bar{p} = \frac{1}{n} \sum_i{x_i}
$$

En suite on prend:

$$
\zeta_{image} = max_ \ d(x_i,\bar{p})
$$

Le problème c'est si on a des outliers. On applique donc la règle de Tukey:
On fait des stats sur les distances et la condition est la suivante

$$
d(x_i,\bar{p}) < Q_1 + 1.5 IQR
$$
où
$$
\\ IQR = Q_3 - Q_1
$$

On enlève les points qui ne respectent pas cette condition, puis on recommence l'algo jusqu'à ce que tous les points respectent cette condition.

En suite on ajoute une marge de sur variance $\alpha$

$$
\zeta_{image}^{final} = \alpha \zeta_{image}
$$

Ici on fixe $\alpha = 1.05$ cette marge réprésente la survariance qu'on autorise