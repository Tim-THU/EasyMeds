package com.timthu.easymeds.integration.neo4j;

import com.timthu.easymeds.config.AppProperties;
import com.timthu.easymeds.domain.CandidateDisease;
import com.timthu.easymeds.domain.GraphContext;
import com.timthu.easymeds.domain.GraphSymptomHint;
import com.timthu.easymeds.service.KnowledgeGraphGateway;
import jakarta.annotation.PreDestroy;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class Neo4jKnowledgeGraphGateway implements KnowledgeGraphGateway {

    private final AppProperties appProperties;
    private volatile Driver driver;

    public Neo4jKnowledgeGraphGateway(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public GraphContext searchContext(Set<String> confirmedSymptoms, Set<String> deniedSymptoms) {
        Driver driver = getDriver();
        if (driver == null || confirmedSymptoms.isEmpty()) {
            return GraphContext.empty(driver != null);
        }

        try (Session session = driver.session(SessionConfig.builder()
                .withDatabase(appProperties.getNeo4j().getDatabase())
                .build())) {

            List<CandidateDisease> candidateDiseases = session.executeRead(tx -> tx.run("""
                            UNWIND $confirmedSymptoms AS symptomName
                            MATCH (s:Symptom {name: symptomName})-[:INDICATES]->(d:Disease)
                            WITH d, collect(DISTINCT symptomName) AS matchedSymptoms, count(DISTINCT symptomName) AS matchedCount
                            RETURN d.name AS disease, matchedSymptoms, matchedCount
                            ORDER BY matchedCount DESC, disease ASC
                            LIMIT $candidateLimit
                            """,
                    Values.parameters(
                            "confirmedSymptoms", List.copyOf(confirmedSymptoms),
                            "candidateLimit", appProperties.getGraphImport().getCandidateLimit()
                    )).list(this::toCandidateDisease));

            List<String> diseaseNames = candidateDiseases.stream().map(CandidateDisease::name).toList();
            if (diseaseNames.isEmpty()) {
                return new GraphContext(true, candidateDiseases, List.of());
            }

            List<GraphSymptomHint> symptomHints = session.executeRead(tx -> tx.run("""
                            MATCH (s:Symptom)-[:INDICATES]->(d:Disease)
                            WHERE d.name IN $diseaseNames
                              AND NOT s.name IN $confirmedSymptoms
                              AND NOT s.name IN $deniedSymptoms
                            RETURN s.name AS symptom, collect(DISTINCT d.name) AS relatedDiseases, count(DISTINCT d) AS supportCount
                            ORDER BY supportCount DESC, symptom ASC
                            LIMIT $symptomLimit
                            """,
                    Values.parameters(
                            "diseaseNames", diseaseNames,
                            "confirmedSymptoms", List.copyOf(confirmedSymptoms),
                            "deniedSymptoms", List.copyOf(deniedSymptoms),
                            "symptomLimit", appProperties.getGraphImport().getSymptomLimit()
                    )).list(this::toSymptomHint));

            return new GraphContext(true, candidateDiseases, symptomHints);
        } catch (Exception ex) {
            return GraphContext.empty(true);
        }
    }

    public void importPairs(List<SymptomDiseasePair> pairs) {
        Driver driver = getDriver();
        if (driver == null) {
            throw new IllegalStateException("Neo4j 未配置，无法导入图谱");
        }
        try (Session session = driver.session(SessionConfig.builder()
                .withDatabase(appProperties.getNeo4j().getDatabase())
                .build())) {
            session.executeWriteWithoutResult(tx -> {
                tx.run("CREATE CONSTRAINT disease_name IF NOT EXISTS FOR (d:Disease) REQUIRE d.name IS UNIQUE");
                tx.run("CREATE CONSTRAINT symptom_name IF NOT EXISTS FOR (s:Symptom) REQUIRE s.name IS UNIQUE");
            });
            session.executeWriteWithoutResult(tx -> {
                for (SymptomDiseasePair pair : pairs) {
                    tx.run("""
                                    MERGE (s:Symptom {name: $symptom})
                                    MERGE (d:Disease {name: $disease})
                                    MERGE (s)-[:INDICATES]->(d)
                                    """,
                            Values.parameters("symptom", pair.symptom(), "disease", pair.disease()));
                }
            });
        }
    }

    private CandidateDisease toCandidateDisease(Record record) {
        List<String> matchedSymptoms = new ArrayList<>();
        for (org.neo4j.driver.Value value : record.get("matchedSymptoms").values()) {
            matchedSymptoms.add(value.asString());
        }
        return new CandidateDisease(
                record.get("disease").asString(),
                record.get("matchedCount").asInt(),
                matchedSymptoms
        );
    }

    private GraphSymptomHint toSymptomHint(Record record) {
        List<String> relatedDiseases = new ArrayList<>();
        for (org.neo4j.driver.Value value : record.get("relatedDiseases").values()) {
            relatedDiseases.add(value.asString());
        }
        return new GraphSymptomHint(
                record.get("symptom").asString(),
                record.get("supportCount").asInt(),
                relatedDiseases
        );
    }

    private Driver getDriver() {
        if (appProperties.getNeo4j().getUri().isBlank()
                || appProperties.getNeo4j().getUsername().isBlank()
                || appProperties.getNeo4j().getPassword().isBlank()) {
            return null;
        }

        if (driver == null) {
            synchronized (this) {
                if (driver == null) {
                    driver = GraphDatabase.driver(
                            appProperties.getNeo4j().getUri(),
                            AuthTokens.basic(appProperties.getNeo4j().getUsername(), appProperties.getNeo4j().getPassword()),
                            Config.builder().withTelemetryDisabled(true).build()
                    );
                }
            }
        }
        return driver;
    }

    @PreDestroy
    public void close() {
        if (driver != null) {
            driver.close();
        }
    }
}