
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class JsonComparator {

    public static class Diff {
        public String path;
        public String file1Value;
        public String file2Value;

        public Diff(String path, String file1Value, String file2Value) {
            this.path = path;
            this.file1Value = file1Value;
            this.file2Value = file2Value;
        }
        public Diff(String path, String file1Value) {
            this.path = path;
            this.file1Value = file1Value;
        }

    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Diff> diffs = new ArrayList<>();

    public List<Diff> compareFiles(File f1, File f2) throws IOException {
        JsonNode j1 = mapper.readTree(f1);
        JsonNode j2 = mapper.readTree(f2);
        compare("", j1, j2);
        return diffs;
    }

    private void compare(String path, JsonNode n1, JsonNode n2) {
        boolean exists1 = n1 != null && !n1.isMissingNode();
        boolean exists2 = n2 != null && !n2.isMissingNode();

        // Missing in one of the files
        if (!exists1 || !exists2) {
            String v1 = exists1 ? renderWithTag(n1) : "<Not Found>";
            String v2 = exists2 ? renderWithTag(n2) : "<Not Found>";
            diffs.add(new Diff(path, v1, v2));
            return;
        }

        // Both arrays: normalize & compare
        if (n1.isArray() && n2.isArray()) {
            List<JsonNode> a1 = normalizeArray(n1), a2 = normalizeArray(n2);
            if (!a1.equals(a2)) {
                compareArrayAndGenerateReport(path, a1,  a2);
            }
            return;
        }

        // Both objects: union of field names
        if (n1.isObject() && n2.isObject()) {
            Set<String> keys = new TreeSet<>();
            n1.fieldNames().forEachRemaining(keys::add);
            n2.fieldNames().forEachRemaining(keys::add);

            for (String key : keys) {
                compare(path.isEmpty() ? key : path + "." + key,
                        n1.has(key) ? n1.get(key) : MissingNode.getInstance(),
                        n2.has(key) ? n2.get(key) : MissingNode.getInstance());
            }
            return;
        }

        // Primitives or mismatched node types
        if (!n1.equals(n2)) {
            diffs.add(new Diff(path,
                    mapper.valueToTree(n1).toString(),
                    mapper.valueToTree(n2).toString()));
        }
    }
    private void compareArrayAndGenerateReport(String path, List<JsonNode> a1, List<JsonNode> a2) {
        for(JsonNode jsonNode: a1) {
            boolean found = false;
            for(JsonNode jsonNode1:a2) {
                if(jsonNode.equals(jsonNode1)) {
                    found =true;
                    break;
                }
            }
            if(!found) {
                diffs.add(new Diff(path,
                        mapper.valueToTree(jsonNode).toString(),
                        mapper.valueToTree("<Not Found>").toString()));
            }
        }
        for(JsonNode jsonNode: a2) {
            boolean found = false;
            for(JsonNode jsonNode1:a1) {
                if(jsonNode.equals(jsonNode1)) {
                    found =true;
                    break;
                }
            }
            if(!found) {
                diffs.add(new Diff(path,
                        mapper.valueToTree("<Not Found>").toString(),
                        mapper.valueToTree(jsonNode).toString()));
            }
        }
    }

    // Sorts and normalizes array elements by their canonical JSON string
    private List<JsonNode> normalizeArray(JsonNode arrayNode) {
        List<JsonNode> list = new ArrayList<>();
        arrayNode.forEach(e -> {
            if (e.isArray()) {
                // nested array: normalize recursively
                list.add(mapper.valueToTree(normalizeArray(e)));
            } else if (e.isObject()) {
                // normalize object: sort fields
                list.add(normalizeObject(e));
            } else {
                list.add(e);
            }
        });
        list.sort(Comparator.comparing(JsonNode::toString));
        return list;
    }

    // Returns an ObjectNode with sorted keys
    private JsonNode normalizeObject(JsonNode obj) {
        Iterator<String> fields = obj.fieldNames();
        TreeMap<String, JsonNode> sorted = new TreeMap<>();
        fields.forEachRemaining(f -> {
            JsonNode v = obj.get(f);
            if (v.isObject())        sorted.put(f, normalizeObject(v));
            else if (v.isArray())    sorted.put(f, mapper.valueToTree(normalizeArray(v)));
            else                     sorted.put(f, v);
        });
        return mapper.valueToTree(sorted);
    }

    // Renders a node as JSON text and appends the "<Not Found>" tag
    private String renderWithTag(JsonNode node) {
        String text = node.isTextual() ? node.textValue() : node.toString();
        return text;
    }

    // Main method: run from CLI
    public static void main(String[] args) throws IOException {
        String file1 = "SampleJson1.json";
        String file2 = "SampleJson2.json";

        File f1 = new File(file1), f2 = new File(file2);
        JsonComparator comp = new JsonComparator();
        List<Diff> diffs = comp.compareFiles(f1, f2);

        // Build report
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("file1", f1.getName());
        report.put("file2", f2.getName());
        report.put("differences", diffs);

        ObjectMapper out = new ObjectMapper();
        System.out.println(out.writerWithDefaultPrettyPrinter()
                .writeValueAsString(report));
    }
}
