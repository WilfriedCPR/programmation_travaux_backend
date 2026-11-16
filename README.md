# GESCLI SONABEL — Backend

## Rôle

API Spring Boot du module de programmation des travaux. Le backend applique les validations métier, les transactions, les autorisations JWT/Keycloak et la persistance JPA.

## Structure

- `src/main/java/.../entity` : entités métier.
- `dto` et `mapper` : contrats REST et conversion.
- `repository` : accès JPA.
- `service` : règles métier et transactions.
- `controller` : routes REST sous `/api`.
- `config/SecurityConfig.java` : CORS, CSRF, JWT et rôles.
- `src/main/resources/application.properties` : configuration externe.

## Versions et configuration

Java 21 et Spring Boot 3.5.6 sont déclarés dans `pom.xml`. La cible de production est PostgreSQL ; les tests d'intégration utilisent une base embarquée isolée lorsqu'ils sont configurés ainsi. Les variables lues sont documentées dans `.env.example` et `application.properties`, notamment `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_JWK_URI`, `KEYCLOAK_TOKEN_URL`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET` et `FILE_UPLOAD_DIR`.

Le port par défaut est `8095`. Le stockage des documents utilise `file.upload-dir` et ne doit jamais pointer vers une production pendant les tests.

## Commandes

Depuis ce dossier :

```powershell
mvn test
mvn -DskipTests package
mvn spring-boot:run
```

Les tests ne nécessitent pas de données SONABEL réelles. Ne pas afficher les secrets du fichier `.env`.

## Sécurité

Les rôles applicatifs sont `ADMIN`, `CHEF` et `AGENT`. Les contrôles d'accès sont appliqués par Spring Security et complétés par les services pour le périmètre des devis, plannings, documents et contraintes. Les fichiers déposés et leurs métadonnées doivent être sauvegardés ensemble.
