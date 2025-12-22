Cahier des charges
1. Introduction
Le présent document décrit les spécifications fonctionnelles et techniques pour le
développement d'une application mobile m-Health destinée aux patients.
Ce projet vise à créer une application mobile permet aux patients :
• d’accéder à leurs informations médicales électroniques,
• de planifier leurs rendez-vous,
• de suivre leurs médicaments,
• et de communiquer avec leurs médecins.
2. Principe conceptuel et fonctionnel :
L'objectif principal de ce projet est de concevoir et de développer une application mHealth pour les patients en réalisant les tâches suivantes :
2.1 Espace patient :
Le patient est l’utilisateur principal de l’application.
Il doit disposer d’un compte personnel (identifiant et mot de passe).
Fonctionnalités principales :
- Avoir un compte patient (login et mot de passe). Le patient doit généralement s'inscrire
à cette application auprès de son fournisseur de soins de santé pour accéder à ses
informations médicales via l'application.
-Consulter ses résultats de laboratoire, son historique médical, ses médicaments
actuels.
- Prendre rendez-vous avec un médecin ou une clinique directement via l’application.
-Planifier, modifier ou annuler des rendez-vous avec ses fournisseurs de soins de santé
directement depuis l'application.
- Demander le renouvellement d’une ordonnance médicale.
- Communiquer avec son médecin (envoi et réception de messages, questions sur le
traitement, conseils médicaux).
- Recevoir des notifications relatives à son traitement, ses rendez-vous médicaux.
2.2 Espace médecin :
Le médecin est un utilisateur autorisé disposant d’un compte sécurisé.
Il accède aux informations médicales de ses patients et peut gérer leur suivi.
Fonctionnalités principales :
- Authentification (login et mot de passe).
-Accéder aux dossiers médicaux électroniques de ses patients (historique, résultats de
laboratoire, rapports d’imagerie).
-Consulter du planning de rendez-vous (date, heure, motif de visite).
-Communiquer avec les patients (Envoyer de messages aux patients pour fournir des
informations, des conseils médicaux ; recevoir les messages de patients et possibilité de
répondre pour des questions médicales ou des demandes de suivi.)
-Consulter et interpréter les résultats médicaux des patients, avec possibilité d’ajouter
des commentaires ou des explications.
2.3 Espace administrateur :
-Gérer des comptes utilisateurs (Créer, modifier et supprimer les comptes utilisateur,
-Attribuer les rôles et les permissions appropriés à chaque utilisateur en fonction de ses
responsabilités et besoins d'accès).
- Gérer la base de données des patients, y compris la création, la modification et la
suppression des dossiers patients, le suivi des autorisations de partage d'informations,
etc.
- Superviser le bon fonctionnement du système et assurer la sécurité et la confidentialité
des données.
2.4 Espace Secrétaire médicale
La secrétaire médicale est une intermédiaire entre les patients et les médecins.
Elle gère les rendez-vous, les dossiers administratifs et peut aider les patients dans la
prise de contact avec les médecins via l’application.
Fonctionnalités principales :
- Gérer des rendez-vous (création, modification, annulation, confirmation).
-Accéder aux profils de patients (informations administratives, coordonnées,
antécédents non médicaux).
-Créer ou mettre à jour un dossier patient à la demande du médecin.
-Consulter les plannings des médecins.
-Transmettre les demandes de rendez-vous ou messages urgents aux médecins.
3. Contraintes techniques
-Plateforme cible : Android
-Langage de développement : Java.
-Environnement de développement : Android Studio