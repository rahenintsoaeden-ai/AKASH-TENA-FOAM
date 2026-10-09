# AKASH & TENA FOAM — GESTION DE STOCK

Application Android native Java, stockage local SQLite, sans connexion Internet nécessaire.

## Fonctionnalités présentes
- 4 catégories et épaisseurs de 8 à 24 cm (pas de 2 cm).
- Modification du stock et du prix par catégorie/épaisseur.
- Vente avec contrôle du stock disponible, enregistrement horodaté et prix historique.
- Entrée de stock.
- Vue stock restant par produit, catégorie et total général.
- Chiffre d'affaires cumulatif par produit et filtres rapides aujourd'hui / 7 jours / mois / période personnalisée.
- Journal de ventes persistant.
- Export des ventes en CSV.

## Compiler l'APK
1. Installer Android Studio sur un ordinateur.
2. Ouvrir le dossier `AkashTenaFoamGestion` dans Android Studio.
3. Laisser Gradle synchroniser le projet (connexion nécessaire pour télécharger les outils la première fois).
4. Choisir **Build > Build APK(s)**.
5. L'APK de débogage sera généré dans `app/build/outputs/apk/debug/app-debug.apk`.

## Limites connues de cette première version
- L'export CSV crée un fichier local et affiche son chemin; le partage direct du fichier via URI n'est pas encore configuré.
- La sauvegarde/restauration complète de la base n'est pas encore implémentée.
- Le journal affiche les ventes, mais les filtres de recherche par catégorie/épaisseur/date ne sont pas encore proposés dans l'interface.
- L'application source est fournie; un APK compilé et testé sur appareil ne peut pas être affirmé sans un environnement Android SDK/Gradle et un essai sur téléphone.
