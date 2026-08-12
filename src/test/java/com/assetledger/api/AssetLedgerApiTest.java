 package com.assetledger.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AssetLedgerApiTest {
    @TempDir
    static Path temporaryDirectory;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "assetledger.database-path",
                () -> temporaryDirectory.resolve("assetledger-api.db").toString()
        );
    }

    @Test
    void exposesHealthAndSummaryEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.ledgerIntegrity").value(true));

        mockMvc.perform(get("/api/v1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetCount", greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.transactionCount", greaterThanOrEqualTo(10)))
                .andExpect(jsonPath("$.ledgerIntegrity").value(true));
    }

    @Test
    void listsAndFiltersSeededAssets() throws Exception {
        mockMvc.perform(get("/api/v1/assets").param("type", "EQUIPMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)));
    }

    @Test
    void registersAndTransfersAnAssetThroughHttp() throws Exception {
        String body = """
                {
                  "type": "INVENTORY",
                  "value": 1200.00,
                  "owner": "QA Warehouse",
                  "metadata": {"sku": "QA-001"}
                }
                """;
        String response = mockMvc.perform(post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentOwner").value("QA Warehouse"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode created = objectMapper.readTree(response);
        String tokenId = created.get("tokenId").asText();

        mockMvc.perform(post("/api/v1/assets/" + tokenId + "/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient": "QA Finance"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromOwner").value("QA Warehouse"))
                .andExpect(jsonPath("$.toOwner").value("QA Finance"));

        mockMvc.perform(get("/api/v1/assets/" + tokenId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentOwner").value("QA Finance"));
    }
}
