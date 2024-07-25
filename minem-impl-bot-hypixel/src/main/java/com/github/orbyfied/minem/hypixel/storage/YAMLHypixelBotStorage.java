package com.github.orbyfied.minem.hypixel.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
@Getter
public class YAMLHypixelBotStorage extends HypixelBotStorage {

    /**
     * The data file.
     */
    final Path path;

    static final Yaml YAML = new Yaml();

    @Override
    public void save() {
        try {
            if (!Files.exists(path)) {
                if (!Files.exists(path.getParent())) {
                    Files.createDirectories(path.getParent());
                }
            }

            OutputStream stream = Files.newOutputStream(path);
            Writer writer = new BufferedWriter(new OutputStreamWriter(stream));
            YAML.dump(saveCompactDefault(), writer);
            stream.close();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save to YAML file " + path, ex);
        }
    }

    @Override
    public void load() {
        try {
            if (!Files.exists(path)) {
                return;
            }

            InputStream stream = Files.newInputStream(path);
            Reader reader = new BufferedReader(new InputStreamReader(stream));
            loadCompactDefault(YAML.load(reader));
            stream.close();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load from YAML file " + path, ex);
        }
    }

}
