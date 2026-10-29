# Le Fil Info — écran 1920 × 1080

Page d’actualités au format paysage 16:9 pour affichage dynamique. Grande actualité à gauche, trois prochains titres à droite, bandeau défilant, horloge française. Mise à l’échelle automatique sans déformation.

**Voir le modèle immédiatement :** après décompression, ouvrez `apercu.html` par un double-clic. Cet aperçu autonome fonctionne sans installation et montre des contenus de démonstration, sans récupérer d’actualités. Pour le site réel, suivez les étapes ci-dessous.

## Installation GitHub, depuis le navigateur

1. Décompressez cette archive sur votre ordinateur.
2. Sur GitHub, créez un dépôt **public**, nommé par exemple `fil-info`. Choisissez `main` comme branche principale et ajoutez un README à la création.
3. Dans le dépôt, cliquez **Add file → Upload files**. Déposez le contenu du dossier : `index.html`, `styles.css`, `app.js`, `config.json`, `README.md` et le dossier `scripts`. Validez avec **Commit changes**.
4. Le dossier caché `.github` peut être omis par le navigateur. Pour créer le workflow de façon fiable : **Add file → Create new file**, saisissez exactement `.github/workflows/actualites.yml`, puis copiez le contenu du fichier `actualites.yml` fourni dans l’archive. Validez avec **Commit changes**.
5. Ouvrez **Settings → Pages → Build and deployment → Source : GitHub Actions**.
6. Ouvrez **Actions → Actualités et publication → Run workflow → Run workflow**. Si GitHub demande d’activer Actions, activez-le. Une première tentative avant le réglage Pages peut échouer ; relancez après l’étape 5.
7. À la fin du traitement, récupérez le lien dans **Settings → Pages**. Il aura la forme `https://VOTRE-PSEUDO.github.io/fil-info/`.
8. Dans Pro Display, ajoutez ce lien comme contenu **page web / URL**. Réglez la zone en **1920 × 1080**. Les libellés exacts et la compatibilité JavaScript dépendent de votre lecteur Pro Display ; vérifiez le rendu sur le lecteur. Si votre playlist interrompt la page, elle peut recommencer au premier article à chaque passage.

Pour voir le design sans actualités réelles, ajoutez `?demo=1` à la fin du lien. **N’utilisez pas ce suffixe sur l’écran définitif.** Les exemples sont clairement signalés et ne représentent pas des nouvelles réelles.

## Fonctionnement

- GitHub Actions récupère les flux chaque heure, à la minute 17. Les horaires planifiés peuvent être retardés par GitHub : ce n’est pas un service en temps réel.
- Le navigateur vérifie les nouvelles données toutes les 15 minutes. Les articles changent toutes les 20 secondes.
- Les quatre sources sont alternées pour que le local et les secours restent visibles.
- Chaque source fournit au maximum 12 articles. Les articles datés de plus de 72 heures sont retirés. Une source publiant peu peut donc ne pas apparaître.
- En cas de panne d’un flux, le workflow reprend les articles récents dans son cache GitHub si ce cache est disponible. Si l’écran perd la connexion, son cache navigateur prend le relais lorsqu’il est disponible.
- La source et la date de publication sont affichées. La date « collecte » indique le dernier passage du programme, pas nécessairement le dernier succès de chaque source. Les pannes partielles sont signalées.
- Les photos sont utilisées seulement lorsqu’une adresse d’image figure dans les champs médias du flux ; si elle manque ou ne charge pas, un fond graphique s’affiche. Aucune photo n’est inventée pour un article.
- Les titres et résumés sont ceux transmis par les éditeurs ; les longs contenus sont limités visuellement à l’espace disponible. Aucun article payant n’est contourné ni récupéré en intégralité.
- Aucun abonnement RSS tiers, clé API ou serveur payant n’est nécessaire pour ce code. Les conditions de votre compte GitHub et des éditeurs restent applicables.

## Sources et diffusion

Les adresses demandées sont préconfigurées dans `config.json` : Le Monde, Franceinfo, Le Dauphiné Isère et Secours Mag. Leur accessibilité peut évoluer. La configuration n’établit pas une autorisation de rediffusion des textes ou images. En particulier, Le Monde réserve ses flux à un usage personnel, non professionnel et non collectif : obtenir leur accord avant une diffusion collective. Les règles des autres éditeurs sont également à respecter.

## Personnaliser

Modifiez `config.json` dans GitHub puis validez. `slideSeconds` : durée d’un article ; `refreshSeconds` : vérification des données par l’écran ; `maxAgeHours` : ancienneté maximale. Pour changer la fréquence de collecte, modifiez le champ `cron` du workflow (UTC, soumis aux règles GitHub).

Le dépôt public et sa page ne doivent contenir que des informations publiques. Aucune information personnelle de l’amicale n’est nécessaire.

## Vérification locale facultative

Avec Python 3.12 et une connexion internet :

```sh
python scripts/update_news.py
python -m http.server 8000 --directory public
```

Ouvrez `http://localhost:8000/` ou `http://localhost:8000/?demo=1`. Un simple double-clic sur le HTML ne permet pas de récupérer les fichiers JSON dans tous les navigateurs.

## Dépannage

- Page 404 : attendre la fin de l’action et vérifier que Source est réglé sur GitHub Actions dans Pages.
- Workflow absent : vérifier le chemin exact `.github/workflows/actualites.yml` et la branche `main`.
- Source indisponible : ouvrir le journal de l’étape « Récupérer les actualités ». Le programme affiche le type d’erreur pour chaque source ; un éditeur peut bloquer les accès automatisés.
- Mise à jour en retard : relancer le workflow. GitHub peut désactiver les workflows planifiés des dépôts publics après une longue période sans activité ; consulter Actions et les réactiver si nécessaire.
- Sans image : c’est normal lorsque le flux ne fournit pas d’image compatible ou que l’éditeur en bloque l’affichage.

Dépôt : https://github.com/remy1838/fil-info . La publication et les mises à jour sont gérées par GitHub Actions.
