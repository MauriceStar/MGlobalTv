# GLOBAL TV Premium 2.0 — Android TV / Google TV

Application Android native en Java, interface paysage optimisée pour Android TV, Google TV et box Android (navigation D-pad/télécommande).

## Fonctionnalités de la version 2.0
- Interface sombre premium avec navigation latérale : Accueil, TV en direct, Films, Séries, Animés, Favoris, Paramètres.
- Recherche globale et filtres de catégories.
- Sources M3U et Xtream Codes.
- Parser M3U (EXTINF, group-title, tvg-logo) et prise en charge des URLs HTTP/HTTPS.
- Xtream : authentification, catégories Live/VOD/Séries, listes de flux, ouverture de fiche série et sélection d'épisodes.
- Lecture via AndroidX Media3 / ExoPlayer avec modules HLS et DASH.
- Favoris enregistrés localement.
- Messages d'erreur et limites de taille réseau pour éviter les réponses anormalement volumineuses.
- Workflow GitHub Actions pour produire automatiquement un APK debug.

## Configuration
1. Ouvrir le dossier dans Android Studio récent.
2. Installer Android SDK Platform 35 et JDK 17.
3. Synchroniser Gradle.
4. Lancer `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
5. L'APK debug est généré dans `app/build/outputs/apk/debug/app-debug.apk`.

Alternative : pousser le dossier sur GitHub puis lancer l'action **Build Android APK** dans l'onglet Actions. Télécharger l'artefact `global-tv-debug-apk`.

## Connexion M3U
Dans Paramètres > Charger M3U, saisir une URL de playlist autorisée. Le catalogue IPTV-org peut être utilisé comme source d'index public. Les flux tiers peuvent être indisponibles, géobloqués ou soumis à des droits.

## Connexion Xtream Codes
Dans Paramètres > Connexion Xtream, saisir l'URL de base du serveur, le nom d'utilisateur et le mot de passe fournis par le fournisseur. L'application appelle les endpoints standards `player_api.php`, `get_live_categories`, `get_live_streams`, `get_vod_categories`, `get_vod_streams`, `get_series_categories`, `get_series` et `get_series_info`.

Les identifiants Xtream ne sont pas enregistrés de façon persistante : ils restent en mémoire pendant la session et doivent être ressaisis après fermeture complète de l'application. HTTPS est fortement recommandé. L'autorisation HTTP claire est activée pour la compatibilité avec certains serveurs IPTV ; ne se connecter qu'à des serveurs de confiance.

## Limites / tests nécessaires
- Aucun contenu premium n'est fourni par l'application. L'utilisateur doit fournir des playlists ou identifiants pour des flux qu'il est autorisé à consulter.
- Certains fournisseurs Xtream utilisent des extensions, structures JSON ou méthodes d'authentification différentes ; compatibilité à valider avec le serveur cible.
- Les affiches/logos distants ne sont pas encore rendus dans les vignettes (les URL sont lues pour préparer l'évolution du catalogue).
- EPG, contrôle parental, reprise de lecture, téléchargement hors ligne et synchronisation cloud ne sont pas inclus dans cette version.
- Aucun APK ne peut être déclaré « zéro crash » sans compilation et essais réels sur plusieurs TV/box, codecs et flux. Effectuer des tests sur les appareils cibles avant distribution.

## Sécurité et droits
GLOBAL TV est un lecteur. Il ne contourne aucun DRM, ne fournit pas de compte IPTV et ne redistribue pas de contenus. N'ajouter que des flux et catalogues dont l'utilisation est autorisée.
