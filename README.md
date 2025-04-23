# PM_Hiver_2025
Dépôt GitHub pour le projet en Programmation Mobile Hiver 2025

Equipe:

Faye Anthonin Devas   DEVA11070200

Léo Moncoiffet        MONL28030200

Martin Chauvelière    CHAM25090200


# Voici une liste de choses importantes à savoir sur l'application DansR et son code

## Général

* Il faut donner l'autorisation de la caméra, de l'accès aux fichiers et du micro afin de pouvoir utiliser l'application.

* L'application contient une limite d'utilisation en temps par jour de 30 minutes. Cette limite est définie ligne 25 du fichier MainActivity.kt. Pour la modifier il suffit de changer le temps ici, en notant que celui-ci est en millisecondes. Les méthodes concernant cette limite sont dans le fichier com.example.dansr/preferences/UsageTracker.kt.

* On utilise les icones de [Google Fonts](https://fonts.google.com/icons).

* Les parties de l'écran qui sont affichées en permanence (top bar, bottom bar) sont définis dans DansRScreen.kt, c'est un des fichiers les plus importants. Il définit également le nom des différentes routes, celles-ci sont appelées selon le modèle "DansRScreen.NomDeLaRoute". 

* La top bar affiche au milieu la page sur laquelle l'utilisateur se trouve actuellement. Le lien entre la route et l'icone se trouve ligne 64 de DansRScreen.kt. 

* Notre application contient 5 pages principales: L'accueil, La galerie, les ressources, l'upload simple, et l'imitation d'une danse existante.

## Écran d'accueil / Scroll

* L'écran d'accueil (DansRScreen.Start) correspond à l'écran où l'on peut scroll les vidéos, les liker, les mettre en "Saved", où les imiter directement. Le code lui correspondant se trouve dans le fichier HomeScreen.kt.

* Mettre une vidéo en "Saved" se fait en appuyant sur l'icone de sablier ou en swipant à gauche. Imiter une danse se fait en appuyant sur l'icone de + ou en swipant à droite. Liker se fait uniquement en appuyant sur l'icone du coeur.

* Swipe vers le haut passe à la vidéo suivante (l'application en prend une aléatoire dans la liste des vidéos existantes). Swipe vers le bas revient à la vidéo précédente s'il y en a une, sinon cela ne fait rien.

* L'overlay est défini ligne 210, les fonctions de swipe dans la box ligne 138. (HomeScreen.kt)

## Galeries

* Il y a 3 galeries différentes dans notre application: 
> * "Liked", le coeur, les vidéos likés.
> * "Saved", le sablier, les vidéos que l'utilisateur "aimerait imiter plus tard".
> * "Uploaded", l'étoile, les vidéos faites par l'utilisateur lui-même. 

* Ces 3 galeries apparaissent dans une seconde top bar qui n'est là que sur cette page. Cette top bar est défini ligne 131 de DansRScreen.kt. 

* On peut passer d'une galerie à l'autre (en plus de le faire en cliquant sur leurs icones dans la seconde top bar) en swipant. Cette fonction existe car chaque galerie est un composable GalleryScreenContent (GalleryScreen.kt, l.80) qui sont créés dans un GalleryPagerScreen (GalleryScreen.kt, l.55).

* Il y a une fonction de cache des miniatures de vidéos (GalleryScreen.kt, l.46) qui permet de garder la miniature de jusqu'à 20 vidéos (le chargement de la galerie était lent sinon et cela causait des petits bugs d'affichages).

## Ressources

* Les ressources sont un endroit où les utilisateurs peuvent retrouver des ressources recommandés pour nous les développeurs afin d'apprendre à mieux dancer. Il y a pour l'instant une redirection vers youtube pour des vidéos, deux sites différents dont Steezy qui est parfait pour un débutant comme pour quelqu'un qui a déjà des bases et finalement il y a aussi une redirection vers une playlist spotify afin de mettre un peu de musique pour dancer chez soi tranquillement.
* Ce système de ressources est simple, il part d'une data class qui regroupe le titre de la ressource, la description de la ressource ainsi que l'url de redirection qui sera utilisé lorsque l'utilisateur clique sur la ressource.
* Il suffit ensuite de rajouter ces données dans les ressources à la suite des autres pour bien ranger le code et ensuite de le rajouter dans la liste d'info card qui se trouve dans le "Datasource" file.

## Upload simple

* L’écran d’upload permet à l’utilisateur d’ajouter ses propres vidéos à l’application via deux méthodes : soit en enregistrant directement une vidéo depuis l’appareil photo, soit en sélectionnant une vidéo existante depuis la galerie.
* Une fois la vidéo sélectionnée ou capturée, une prévisualisation est affichée à l’utilisateur grâce à un composant VideoPlayer.
* Deux options sont ensuite proposées :
  * Recommencer l’enregistrement ou la sélection via l’icône de relecture.
  * Publier la vidéo via l’icône d’upload, ce qui déclenche la fonction publishUserVideo, puis redirige automatiquement vers l’écran d’accueil (DansRScreen.Start).
* La capture vidéo utilise le système de cache pour stocker temporairement les fichiers. L’URI du fichier est géré avec un FileProvider pour assurer la compatibilité Android.

## Imiter une danse

* Cette fonctionnalité permet à l'utilisateur de visionner une vidéo modèle (fournie dans les assets), puis d'enregistrer sa propre tentative d’imitation directement via la caméra.
* L’expérience utilisateur se déroule en trois étapes :
  * Lecture du modèle avec des contrôles de lecture (play/pause, replay, démarrer imitation).
  * Enregistrement vidéo via la caméra intégrée.
  * Comparaison des deux vidéos :
    * Les vidéos sont affichées simultanément (grande + vignette), avec possibilité de les inverser d’un simple tap.
    * Les vidéos se relancent automatiquement en boucle à la fin pour une meilleure comparaison.
    * L'utilisateur peut rejouer l'enregistrement ou publier sa vidéo via les boutons en surimpression.
* Une fois publiée, la vidéo est copiée dans un répertoire interne et ajoutée au fichier status.json avec le flag isUploaded = true.
* L'utilisation d'ExoPlayer permet une lecture fluide à partir des fichiers locaux, qu'ils proviennent des assets ou du stockage interne.

## Gestion des fichiers vidéos

* Nos fichiers de vidéos sont stockés en local, nous n'avons rien en dehors de l'application elle-même.

* Les vidéos sont toutes dans un dossier assets/videos.

* La gestion de quelles vidéos vont dans quelles galeries se fait grâce à un fichier status.json contenant pour chaque vidéo les informations suivantes: nom, isLiked, isSaved, isUploaded (VideoStatus.kt, l.77). La fonction créant ce fichier json est appelée dans le onCreate de l'application (MainActivity.kt, l.38).

* Le fichier VideoStatus.kt contient toutes les méthodes concernant ce fichier, le changement d'états des vidéos (si l'utilisateur les like ou les mets en saved).
