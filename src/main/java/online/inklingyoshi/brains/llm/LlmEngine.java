package online.inklingyoshi.brains.llm;

import com.google.gson.*;
import online.inklingyoshi.brains.ai.AiProfile;
import online.inklingyoshi.brains.gamestate.GameState;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * LLM Engine using Siliconflow (DeepSeek 3.2) for AI decision making.
 */
public class LlmEngine {
    private static final String API_KEY = System.getenv("SILICONFLOW_API_KEY");
    private static final String BASE_URL = "https://api.siliconflow.cn/v1";
    private static final String MODEL = "deepseek-ai/DeepSeek-V3";
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static AiDecision decideAction(GameState state, AiProfile ai) {
        try {
            LlmRequest req = buildRequest(state, ai);
            String json = GSON.toJson(req);
            
            String response = sendRequest(json);
            if (response == null || response.isEmpty()) {
                return AiDecision.doNothing();
            }
            
            return parseDecision(response);
        } catch (Exception e) {
            System.err.println("[WatheBrains] LLM request failed: " + e.getMessage());
            return AiDecision.doNothing();
        }
    }

    private static LlmRequest buildRequest(GameState state, AiProfile ai) {
        LlmRequest req = new LlmRequest();
        req.model = MODEL;
        req.messages = new ArrayList<>();
        req.tools = List.of(buildDecideActionTool());
        req.tool_choice = "auto";
        
        LlmRequest.Message sys = new LlmRequest.Message();
        sys.role = "system";
        sys.content = SystemPromptBuilder.buildForRole(ai.getRole());
        req.messages.add(sys);
        
        LlmRequest.Message user = new LlmRequest.Message();
        user.role = "user";
        user.content = GSON.toJson(state);
        req.messages.add(user);
        
        return req;
    }

    private static LlmRequest.Tool buildDecideActionTool() {
        LlmRequest.Tool tool = new LlmRequest.Tool();
        tool.type = "function";
        
        LlmRequest.ToolFunction func = new LlmRequest.ToolFunction();
        func.name = "decide_action";
        func.description = "Choose the next action for the AI player based on the game state.";
        
        JsonObject props = new JsonObject();
        
        JsonObject actionProp = new JsonObject();
        actionProp.addProperty("type", "string");
        JsonArray actionEnum = new JsonArray();
        actionEnum.add("move_to"); actionEnum.add("chat"); actionEnum.add("follow_player");
        actionEnum.add("get_snack"); actionEnum.add("get_drink"); actionEnum.add("get_air");
        actionEnum.add("get_sleep"); actionEnum.add("buy_item"); actionEnum.add("knife_kill");
        actionEnum.add("grenade_kill"); actionEnum.add("activate_psycho"); actionEnum.add("poison_food");
        actionEnum.add("poison_bed"); actionEnum.add("blackout"); actionEnum.add("do_nothing");
        actionProp.add("enum", actionEnum);
        props.add("action", actionProp);
        
        JsonObject targetPlayerProp = new JsonObject();
        targetPlayerProp.addProperty("type", "string");
        targetPlayerProp.addProperty("description", "Target player name");
        props.add("target_player", targetPlayerProp);
        
        JsonObject targetLocationProp = new JsonObject();
        targetLocationProp.addProperty("type", "string");
        targetLocationProp.addProperty("description", "Target location");
        props.add("target_location", targetLocationProp);
        
        JsonObject itemProp = new JsonObject();
        itemProp.addProperty("type", "string");
        JsonArray itemEnum = new JsonArray();
        itemEnum.add("knife"); itemEnum.add("lockpick"); itemEnum.add("revolver");
        itemEnum.add("poison_vial"); itemEnum.add("scorpion");
        itemProp.add("enum", itemEnum);
        props.add("item", itemProp);
        
        JsonObject messageProp = new JsonObject();
        messageProp.addProperty("type", "string");
        messageProp.addProperty("description", "Chat message content");
        props.add("message", messageProp);
        
        JsonObject paramsSchema = new JsonObject();
        paramsSchema.add("type", new JsonPrimitive("object"));
        paramsSchema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("action");
        paramsSchema.add("required", required);
        
        func.parameters = paramsSchema;
        tool.function = func;
        
        return tool;
    }

    private static String sendRequest(String json) throws Exception {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("[WatheBrains] SILICONFLOW_API_KEY not set!");
            return null;
        }
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(BASE_URL + "/chat/completions");
            post.setHeader("Authorization", "Bearer " + API_KEY);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = client.execute(post)) {
                String body = EntityUtils.toString(response.getEntity());
                
                if (response.getStatusLine().getStatusCode() != 200) {
                    System.err.println("[WatheBrains] API error: " + response.getStatusLine().getStatusCode());
                    System.err.println("[WatheBrains] Response: " + body);
                    return null;
                }
                
                return body;
            }
        }
    }

    private static AiDecision parseDecision(String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            
            if (choices == null || choices.size() == 0) return AiDecision.doNothing();
            
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message == null) return AiDecision.doNothing();
            
            JsonArray toolCalls = message.getAsJsonArray("tool_calls");
            if (toolCalls == null || toolCalls.size() == 0) return AiDecision.doNothing();
            
            JsonObject toolCall = toolCalls.get(0).getAsJsonObject();
            JsonObject function = toolCall.getAsJsonObject("function");
            String argumentsStr = function.get("arguments").getAsString();
            
            return GSON.fromJson(argumentsStr, AiDecision.class);
        } catch (Exception e) {
            System.err.println("[WatheBrains] Failed to parse LLM response: " + e.getMessage());
            return AiDecision.doNothing();
        }
    }

    public static class LlmRequest {
        public String model;
        public List<Message> messages;
        public List<Tool> tools;
        public String tool_choice;
        
        public static class Message {
            public String role;
            public String content;
        }
        
        public static class Tool {
            public String type;
            public ToolFunction function;
        }
        
        public static class ToolFunction {
            public String name;
            public String description;
            public JsonObject parameters;
        }
    }

    public static class AiDecision {
        public String action;
        public String target_player;
        public String target_location;
        public String item;
        public String message;
        
        public static AiDecision doNothing() {
            AiDecision d = new AiDecision();
            d.action = "do_nothing";
            return d;
        }
    }

    private static class SystemPromptBuilder {
        public static String buildForRole(String role) {
            String base = """
                You are playing a game on a train (Wathe / Harpy Express).
                Your goal depends on your role.
                
                Actions: move_to, chat, follow_player, get_snack, get_drink, get_air, get_sleep,
                buy_item, knife_kill, grenade_kill, activate_psycho, poison_food, poison_bed, blackout, do_nothing
                
                Important: Manage mood as civilian/vigilante. Killers have wallhack but must fake tasks.
                """;
            
            return switch (role.toLowerCase()) {
                case "killer" -> base + "\nKILLER: Buy weapons, kill players, poison food/beds, use psycho mode.";
                case "vigilante" -> base + "\nVIGILANTE: Like civilian but can attack suspicious players.";
                case "civilian" -> base + "\nCIVILIAN: Maintain mood with snacks/drink/air/sleep. Avoid danger!";
                default -> base;
            };
        }
    }
}
