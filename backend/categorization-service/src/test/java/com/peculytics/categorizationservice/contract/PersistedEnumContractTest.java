package com.peculytics.categorizationservice.contract;

import com.peculytics.categorizationservice.model.AnalysisStatus;
import com.peculytics.categorizationservice.model.StatementFileStatus;
import com.peculytics.categorizationservice.model.TransactionCategory;
import com.peculytics.categorizationservice.model.TransactionCategorySource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PersistedEnumContractTest {
    private static final String API_MODEL_PATH =
            "backend/api-service/src/main/java/com/peculytics/apiservice/model/";

    @Test
    void categorizationRuleSeedCategoriesMustExistInTransactionCategory() {
        Set<String> seedCategories = seedCategories("infra/migrations/V4__create_categorization_rules_table.sql");

        assertThat(seedCategories)
                .isNotEmpty()
                .isSubsetOf(enumNames(TransactionCategory.class));
    }

    @Test
    void transactionCategoryEnumsMustMatchBetweenApiAndCategorizationServices() {
        assertThat(apiEnumNames("TransactionCategory"))
                .containsExactlyElementsOf(enumNames(TransactionCategory.class));
    }

    @Test
    void transactionCategorySourceEnumsMustMatchBetweenApiAndCategorizationServices() {
        assertThat(migration("infra/migrations/V3__create_transactions_table.sql"))
                .contains("category_source VARCHAR(255) NOT NULL");

        assertThat(apiEnumNames("TransactionCategorySource"))
                .containsExactlyElementsOf(enumNames(TransactionCategorySource.class));
    }

    @Test
    void analysisStatusEnumsMustMatchBetweenApiAndCategorizationServices() {
        assertThat(migration("infra/migrations/V1__create_analyses_table.sql"))
                .contains("status VARCHAR(255) NOT NULL");

        assertThat(apiEnumNames("AnalysisStatus"))
                .containsExactlyElementsOf(enumNames(AnalysisStatus.class));
    }

    @Test
    void statementFileStatusEnumsMustMatchBetweenApiAndCategorizationServices() {
        assertThat(migration("infra/migrations/V2__create_statement_files_table.sql"))
                .contains("status VARCHAR(255) NOT NULL");

        assertThat(apiEnumNames("StatementFileStatus"))
                .containsExactlyElementsOf(enumNames(StatementFileStatus.class));
    }

    private static Set<String> seedCategories(String relativePath) {
        Pattern tuple = Pattern.compile("\\('(?:''|[^'])*'\\s*,\\s*'([A-Z_]+)'\\)");
        Matcher matcher = tuple.matcher(migration(relativePath));
        Set<String> categories = new TreeSet<>();

        while (matcher.find()) {
            categories.add(matcher.group(1));
        }

        return categories;
    }

    private static Set<String> apiEnumNames(String enumName) {
        return javaEnumNames(API_MODEL_PATH + enumName + ".java", enumName);
    }

    private static Set<String> javaEnumNames(String relativePath, String enumName) {
        String source = readProjectFile(relativePath)
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)//.*$", "");
        Matcher enumMatcher = Pattern.compile("(?s)enum\\s+" + enumName + "\\s*\\{(.*)}").matcher(source);
        assertThat(enumMatcher.find())
                .as("Expected enum %s in %s", enumName, relativePath)
                .isTrue();

        String constantsBlock = enumMatcher.group(1);
        int constantsEnd = constantsBlock.indexOf(';');
        if (constantsEnd >= 0) {
            constantsBlock = constantsBlock.substring(0, constantsEnd);
        }

        return Arrays.stream(constantsBlock.split(","))
                .map(String::trim)
                .map(PersistedEnumContractTest::enumConstantName)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static String enumConstantName(String declaration) {
        Matcher matcher = Pattern.compile("^([A-Z][A-Z0-9_]*)").matcher(declaration);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static Set<String> enumNames(Class<? extends Enum<?>> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static String migration(String relativePath) {
        return readProjectFile(relativePath);
    }

    private static String readProjectFile(String relativePath) {
        Path projectRoot = projectRoot();
        try {
            return Files.readString(projectRoot.resolve(relativePath));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read project file: " + relativePath, exception);
        }
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("infra/migrations"))
                    && Files.exists(current.resolve("backend/api-service"))
                    && Files.exists(current.resolve("backend/categorization-service"))) {
                return current;
            }
            current = current.getParent();
        }

        throw new IllegalStateException("Could not locate Peculytics project root");
    }
}
