package com.appsmith.server.migrations.db.ce;

import com.appsmith.external.models.PluginType;
import com.appsmith.server.domains.Plugin;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;

import static com.appsmith.server.migrations.DatabaseChangelog1.installPluginToAllWorkspaces;

@Slf4j
@ChangeUnit(order = "047", id = "add-bonita-plugin", author = " ")
public class Migration047AddBonitaPlugin {

    private final MongoTemplate mongoTemplate;

    public Migration047AddBonitaPlugin(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @RollbackExecution
    public void rollbackExecution() {}

    @Execution
    public void addBonitaPlugin() {
        Plugin plugin = new Plugin();
        plugin.setName("Bonita");
        plugin.setType(PluginType.API);
        plugin.setPackageName("bonita-plugin");
        plugin.setUiComponent("ApiEditorForm");
        plugin.setDatasourceComponent("RestAPIDatasourceForm");
        plugin.setResponseType(Plugin.ResponseType.JSON);
        plugin.setIconLocation("https://cdn3.bonitasoft.com/sites/default/files/Bonitasoft_Logo_Bulle.svg");
        plugin.setDocumentationLink("https://api-documentation.bonitasoft.com/latest/");
        plugin.setDefaultInstall(true);
        try {
            mongoTemplate.insert(plugin);
        } catch (DuplicateKeyException e) {
            log.warn(plugin.getPackageName() + " already present in database.");
        }

        if (plugin.getId() == null) {
            log.error("Failed to insert the Bonita plugin into the database.");
            return;
        }

        installPluginToAllWorkspaces(mongoTemplate, plugin.getId());
    }

}
