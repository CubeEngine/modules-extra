/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.discord;

import org.cubeengine.reflect.Section;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiscordConfig extends ReflectedYaml {

    public String applicationId;
    public String botToken;
    public String channel = "minecraft";
    public String defaultChatFormat = "{NAME} from &5Discord&r: {MESSAGE}";
    public boolean replaceEmoji = true;
    public Map<String, String> emojiMapping = new HashMap<>();

    public WebhookConfig webhook = new WebhookConfig();

    public transient Pattern forwardEmojiReplacePattern = null;

    public static class WebhookConfig implements Section {
        public String id;
        public String token;
    }

    public DiscordConfig() {
        this.emojiMapping.put("👋", "o/");
        this.emojiMapping.put("💔", "</3");
        this.emojiMapping.put("💗", "<3");
        this.emojiMapping.put("\uD83D\uDE1B", ":P");
        this.emojiMapping.put("\uD83D\uDE42", ":)");
        this.emojiMapping.put("\uD83D\uDE04", ":D");
        this.emojiMapping.put("\uD83D\uDE09", ";)");
        this.emojiMapping.put("\uD83D\uDE10", ":|");
        this.emojiMapping.put("☹️", ":(");
        this.emojiMapping.put("\uD83D\uDE15", ":-/");
        this.emojiMapping.put("\uD83D\uDE12", ":-S");
        this.emojiMapping.put("\uD83D\uDE24", ">_>^");
        this.emojiMapping.put("😃", ":-))");
        this.emojiMapping.put("😇", "0:-)");
        this.emojiMapping.put("😳", ":$");
        this.emojiMapping.put("😵", "#-)");
        this.emojiMapping.put("😶", ":X");
        this.emojiMapping.put("😼", ":-J");
        this.emojiMapping.put("😽", ":*");
        this.emojiMapping.put("🙅", "ಠ_ಠ");
        this.emojiMapping.put("🙆", "\\o/");
        this.emojiMapping.put("😈", "}:-)");
        this.emojiMapping.put("😁", "xD");
    }



    @Override
    public void onLoad() {
        this.forwardEmojiReplacePattern = compileToPattern(this.emojiMapping.keySet());
    }

    private static Pattern compileToPattern(Set<String> options) {
        return Pattern.compile(options.stream().map(Pattern::quote).collect(Collectors.joining("|", "(?:", ")")));
    }
}
