package tech.jorn.adrian.experiment;

import org.yaml.snakeyaml.Yaml;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureNode;
import tech.jorn.adrian.core.graphs.infrastructure.SoftwareAsset;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class InfrastructureLoader {
    public static Infrastructure loadFromYaml(String filePath) {
        var yaml = new Yaml();
        InputStream inputStream = InfrastructureLoader.class
                .getClassLoader()
                .getResourceAsStream(filePath);
        Map<String, Object> content = yaml.load(inputStream);

        List<InfrastructureNode> nodes = new ArrayList<>();
        {
            var _nodes = (List<Map<String, Object>>) content.getOrDefault("nodes", new ArrayList<>());
            _nodes.forEach(info -> {
                var node = new InfrastructureNode((String) info.get("id"));
                info.remove("id");
                info.remove("name");
                info.forEach((key, value) -> {
                    if (value instanceof Double) node.setProperty(key, ((Double) value).floatValue());
                    if (value instanceof String) node.setProperty(key, (String) value);
                    else node.setProperty(key, value);
                });
                nodes.add(node);
            });
        }

        List<SoftwareAsset> assets = new ArrayList<>();
        {
            var _assets = (List<Map<String, Object>>) content.getOrDefault("assets", new ArrayList<>());
            _assets.forEach(info -> {
                var asset = new SoftwareAsset((String) info.get("id"));
                info.remove("id");
                info.remove("name");
                info.forEach((key, value) -> {
                    if (value instanceof Double) asset.setProperty(key, ((Double) value).floatValue());
                    else asset.setProperty(key, value);
                });
                assets.add(asset);
            });
        }

        List<Link> links = new ArrayList<>();
        {
            var _links = (List<Map<String, Object>>) content.getOrDefault("links", new ArrayList<>());
            _links.forEach(info -> {
                var link = new Link((String) info.get("source"), (String) info.get("target"));
                links.add(link);
            });
        }
        List<Link> connectors = new ArrayList<>();
        {
            var _connectors = (List<Map<String, Object>>) content.getOrDefault("connectors", new ArrayList<>());
            _connectors.forEach(info -> {
                var link = new Link((String) info.get("source"), (String) info.get("target"));
                connectors.add(link);
            });
        }

        var infrastructure = new Infrastructure();
        nodes.forEach(infrastructure::upsertNode);
        assets.forEach(asset -> {
            var host = asset.getProperty("host");
            if (host.isEmpty()) return;

            var source = infrastructure.findById((String) host.get());
            if (source.isEmpty()) return;

            infrastructure.upsertNode(asset);
            infrastructure.addEdge(source.get(), asset);
            infrastructure.addEdge(asset, source.get());
        });

        Stream.concat(links.stream(),connectors.stream()).forEach(link -> {
            var source = infrastructure.findById(link.source());
            var target = infrastructure.findById(link.target());
            if (source.isEmpty() || target.isEmpty()) return;

            infrastructure.addEdge(source.get(), target.get());
            infrastructure.addEdge(target.get(), source.get());
        });
        return infrastructure;
    }
}

record Link(String source, String target) { }