# GTA VI Widget pour Samsung

Application personnelle sans publicité. Elle conserve la page https://remy1838.github.io/fil-info/gta6/ comme source de configuration et destination au toucher.

Le widget utilise le Chronometer Android natif pour les secondes, au lieu de captures de page web. Jours en grand, heures/minutes/secondes en dessous, trois visuels Rockstar aux plages 00–08 h, 08–16 h et 16–00 h (Europe/Paris). Les alarmes précises doivent être autorisées pour les transitions aux horaires prévus. Les images sont incluses et fonctionnent hors connexion; le bouton de synchronisation relit widget.json sous le lien existant.

Installation : ouvrir le fichier APK sur Samsung, autoriser cette installation ponctuelle si demandé, ouvrir GTA VI Widget, autoriser les horaires précis, puis Ajouter le widget. Redimensionner à 4 × 3. Tester le défilement sur le téléphone : une validation sur émulateur ne garantit pas le comportement de One UI.

Compilation : Gradle 8.10.2, Java 17, Android SDK 35; gradle :app:assembleRelease. APK personnel signé avec une clé de développement. Pour diffuser ou mettre à jour durablement cette application, conserver une clé de signature privée stable. Aucune publication Play Store.

Visuels et logo © Rockstar Games / Take-Two. Application de fan.
