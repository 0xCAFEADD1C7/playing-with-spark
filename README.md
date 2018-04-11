Marco PONTI
Aymeric ROBINI

# Projet DAR - Emergency Arrival Machine Learning

## Introduction

À la recherche de données qui nous sembleraient adéquates à une monétisation, nous sommes tombés (grâce à Kaggle) sur les données FARS (Fatality Analysis Reporting System), qui regroupent de nombreuses informations, notemment sur les accidents de la route aux états unis. Nous nous sommes initialement lancés dans l'idée d'utiliser ces données pour une compagnie d'assurance, plus précisément dans l'optique d'évaluer les risques d'une location automobile. Bien qu'étant particulièrement interessant d'un point de vue statistique, ce sujet ne representait pour nous pas un grand interet technique. Nous avons donc décidé de garder ces données, mais d'essayer de les inclure dans une démarche de machine learning. Une problèmatique s'en est tout naturellement dégagée : pour un accident qui vient d'avoir lieu, peut on le plus précisément possible prédire le temps d'arrivée des secours ?

## Le dataset

Comme dit précédemment, le dataset concerne essentiellement des données liées aux accidents de la route. Parmi les données fournies, nous pouvons trouver la date de l'accident, la position géographique, le nombre de décès, le temps d'arrivée des secours (pas toujours renseigné malheureusement), ainsi que divers détails comme la météo, le type de route, l'état, etc.

## Traitements préalables

Notre problématique s'interesse au temps d'arrivée des secours. Toutefois, cette donnée n'était pas representée dans un format directement utilisable, il a donc fallu la transformer pour qu'à partir d'une heure d'accident et d'une heure d'arriver des secours, nous soyons capables d'obtenir directement le nombre de minutes écoulées.

De plus, l'heure d'arrivée des secours n'étant pas toujours renseignée, il a fallu filtrer les lignes où ces données étaient absentes.

## Méthode

Tout au long du développement de ce projet, nous avons suivi la ligne directrice suivante ; nous prenons 95% de notre dataset, le donnons en *entrainement* à notre modèle linéaire (potentiellement, on transforme les colonnes catégorielles en colonnes binaires si besoin), puis, sur les 5% restants, on essaye de prédire la donnée que nous recherchons, et comparons cette valeur à la vraie valeur.

Pour comparer, nous utiliserons l'erreur quadratique moyenne (qu'on appellera *RMSE*).

Notre objectif au long de ce projet est de minimiser cette métrique.

## Expérimentations

Désormais nous allons nous interesser aux données que nous allons donner en entrée à notre modèle (nos *features*), et essayer de trouver les plus pertinentes dans notre prédiction. 

En suivant la démarche précédente, nous avons très vite fait face à des RMSE très élevés. En effet, des cas très exceptionnels faussaient les résultats ; par exemple, il existe des cas où les secours ont mis jusqu'à 10h pour arriver. De tels cas sont très difficilement prévisibles de par leur faible fréquence d'arrivée, mais comme nous mesurons l'écart quadratique, le RMSE est particulièrement impacté. Nous avons donc choisi de filtrer ces lignes de notre dataset.

Pensant que la date aurait potentiellement une importance, nous avons tout d'abord essayé de remplir notre modèle avec les features de date, mais cela ne fut pas concluant.
Dans la même lancée, nous avons supposé que l'heure impacterait. Nous avons donc essayé 3 méthodes :
- Donner l'heure comme variable numérique. Les résultats n'ont pas été concluants. En effet ce serait un non sens de dire "plus l'heure est élevée, plus le temps de secours est plus ou moins élevé", puisque cela impliquerait par exemple que 00h et 23h sont très différentes. 
- Pour compenser ce phénomène, nous avons essayé de donner une valeur graduelle aux heures, en se partant de la reflexion "plus on est dans la nuit, plus le temps de secours pourrait être impacté". Pour implémenter cela, nous avons donc calculé le nombre d'heures d'écart avec le "milieu de la nuit", que nous avons estimé à 3h du matin. Malheuresement celui ci n'a pas non plus été concluant.
- Enfin, nous avons donné l'heure en tant que variable catégorielle. Cela nous as permis une amélioration de la prédiction. En effet, on peut penser par exemple que dans les pics d'affluence (donc catégories 8h, 9h, et 17h, 18h par exemple), impacterons sur le trafic et donc le temps d'arrivée.

Ensuite, nous avons pensé que le deuxième facteur le plus important serait un facteur géographique.
Nous avons donc essayé d'ajouter différents features étant plus ou moins directement liées à la position géographique. Par exemple, celle qui nous a paru la plus intéressante était le nom de la route. Si celle ci, était surement la plus précise pour nos éstimations, un problème se pose : cette variable est nécéssairement catégorielle, or, pour utiliser une variable catégorielle dans une prédiction, il faut que celle ci apparaisse dans le trainset. Or, il n'est pas possible d'avoir des données d'accident sur toutes les routes des états unis.

Ensuite, nous avons pensé que si les secours partent des hopitaux, alors il parait probable que la distance entre l'hopital le plus proche et le lieu de l'accident pourrait avoir un poids important pour la prédiction. Par chance, nous avons trouvé un dataset des hopitaux des États Unis (fichier *data/hospital.csv*). Nous avons donc créé un script pour ajouter la distance avec l'hopital le plus proche dans notre dataset des accidents (fichier *data/accident_hosp.csv*). Étonnement, cette donnée apporte un effet négatif à notre prédiction. 

Enfin, nous avons essayé d'ajouter des features successivement, sans critères de choix particuliers, en retenant celles qui améliorent le plus notre prédicition. Les features que nous avons au final retenues sont donc l'heure, le type de route (urbain/rural), la météo au moment de l'accident, les conditions lumineuses, le nombre de personnes impliquées, et les types d'intersection s'il y a lieu.

### Améliorations possibles

Les fonctions développées nous permettent des estimations plus ou moins vraies. Nous avons donc penser à quelques idées qui nous permettraient d'améliorer notre prédiction :

* Comme dit précédemment, nous n'avons utilisé que le modèle de régression linéaire, mais il pourrait être interessant d'essayer avec un apprentissage type *Random Forest*.

* Certains essais se sont basés sur la localisation des routes. Une approche envisageable serait de calculer la distance avec la route connue (c'est à dire une route où il y a déjà eu des accidents) la plus proche, et d'inclure cette distance dans les features.