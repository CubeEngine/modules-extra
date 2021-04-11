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
    public boolean replaceEmojiFromMinecraft = false;
    public Map<String, String> emojiMapping = new HashMap<>();

    public WebhookConfig webhook = new WebhookConfig();


    public transient Pattern forwardEmojiReplacePattern = null;
    public transient Map<String, String> reverseEmojiMapping = new HashMap<>();
    public transient Pattern reverseEmojiReplacePattern = null;

    public static class WebhookConfig implements Section {
        public String id;
        public String token;
    }

    public DiscordConfig() {
        // See: https://stackoverflow.com/a/29581503/1827771
        this.emojiMapping.put("👋", "o/");
        this.emojiMapping.put("💔", "</3");
        this.emojiMapping.put("💗", "<3");
        this.emojiMapping.put("😁", "8-D");
        this.emojiMapping.put("😁", "8D");
        this.emojiMapping.put("😁", ":-D");
        this.emojiMapping.put("😁", "=-3");
        this.emojiMapping.put("😁", "=-D");
        this.emojiMapping.put("😁", "=3");
        this.emojiMapping.put("😁", "=D");
        this.emojiMapping.put("😁", "B^D");
        this.emojiMapping.put("😁", "X-D");
        this.emojiMapping.put("😁", "XD");
        this.emojiMapping.put("😁", "x-D");
        this.emojiMapping.put("😁", "xD");
        this.emojiMapping.put("😂", ":')");
        this.emojiMapping.put("😂", ":'-)");
        this.emojiMapping.put("😃", ":-))");
        this.emojiMapping.put("😄", "8)");
        this.emojiMapping.put("😄", ":)");
        this.emojiMapping.put("😄", ":-)");
        this.emojiMapping.put("😄", ":3");
        this.emojiMapping.put("😄", ":D");
        this.emojiMapping.put("😄", ":]");
        this.emojiMapping.put("😄", ":^)");
        this.emojiMapping.put("😄", ":c)");
        this.emojiMapping.put("😄", ":o)");
        this.emojiMapping.put("😄", ":}");
        this.emojiMapping.put("😄", ":っ)");
        this.emojiMapping.put("😄", "=)");
        this.emojiMapping.put("😄", "=]");
        this.emojiMapping.put("😇", "0:)");
        this.emojiMapping.put("😇", "0:-)");
        this.emojiMapping.put("😇", "0:-3");
        this.emojiMapping.put("😇", "0:3");
        this.emojiMapping.put("😇", "0;^)");
        this.emojiMapping.put("😇", "O:-)");
        this.emojiMapping.put("😈", "3:)");
        this.emojiMapping.put("😈", "3:-)");
        this.emojiMapping.put("😈", "}:)");
        this.emojiMapping.put("😈", "}:-)");
        this.emojiMapping.put("😉", "*)");
        this.emojiMapping.put("😉", "*-)");
        this.emojiMapping.put("😉", ":-,");
        this.emojiMapping.put("😉", ";)");
        this.emojiMapping.put("😉", ";-)");
        this.emojiMapping.put("😉", ";-]");
        this.emojiMapping.put("😉", ";D");
        this.emojiMapping.put("😉", ";]");
        this.emojiMapping.put("😉", ";^)");
        this.emojiMapping.put("😐", ":-|");
        this.emojiMapping.put("😐", ":|");
        this.emojiMapping.put("😒", ":(");
        this.emojiMapping.put("😒", ":-(");
        this.emojiMapping.put("😒", ":-<");
        this.emojiMapping.put("😒", ":-[");
        this.emojiMapping.put("😒", ":-c");
        this.emojiMapping.put("😒", ":<");
        this.emojiMapping.put("😒", ":[");
        this.emojiMapping.put("😒", ":c");
        this.emojiMapping.put("😒", ":{");
        this.emojiMapping.put("😒", ":っC");
        this.emojiMapping.put("😖", "%)");
        this.emojiMapping.put("😖", "%-)");
        this.emojiMapping.put("😜", ":-P");
        this.emojiMapping.put("😜", ":-b");
        this.emojiMapping.put("😜", ":-p");
        this.emojiMapping.put("😜", ":-Þ");
        this.emojiMapping.put("😜", ":-þ");
        this.emojiMapping.put("😜", ":P");
        this.emojiMapping.put("😜", ":b");
        this.emojiMapping.put("😜", ":p");
        this.emojiMapping.put("😜", ":Þ");
        this.emojiMapping.put("😜", ":þ");
        this.emojiMapping.put("😜", ";(");
        this.emojiMapping.put("😜", "=p");
        this.emojiMapping.put("😜", "X-P");
        this.emojiMapping.put("😜", "XP");
        this.emojiMapping.put("😜", "d:");
        this.emojiMapping.put("😜", "x-p");
        this.emojiMapping.put("😜", "xp");
        this.emojiMapping.put("😠", ":-||");
        this.emojiMapping.put("😠", ":@");
        this.emojiMapping.put("😡", ":-.");
        this.emojiMapping.put("😡", ":-/");
        this.emojiMapping.put("😡", ":/");
        this.emojiMapping.put("😡", ":L");
        this.emojiMapping.put("😡", ":S");
        this.emojiMapping.put("😡", ":\\");
        this.emojiMapping.put("😡", "=/");
        this.emojiMapping.put("😡", "=L");
        this.emojiMapping.put("😡", "=\\");
        this.emojiMapping.put("😢", ":'(");
        this.emojiMapping.put("😢", ":'-(");
        this.emojiMapping.put("😤", "^5");
        this.emojiMapping.put("😤", "^<_<");
        this.emojiMapping.put("😤", "o/\\o");
        this.emojiMapping.put("😫", "|-O");
        this.emojiMapping.put("😫", "|;-)");
        this.emojiMapping.put("😰", ":###..");
        this.emojiMapping.put("😰", ":-###..");
        this.emojiMapping.put("😱", "D-':");
        this.emojiMapping.put("😱", "D8");
        this.emojiMapping.put("😱", "D:");
        this.emojiMapping.put("😱", "D:<");
        this.emojiMapping.put("😱", "D;");
        this.emojiMapping.put("😱", "D=");
        this.emojiMapping.put("😱", "DX");
        this.emojiMapping.put("😱", "v.v");
        this.emojiMapping.put("😲", "8-0");
        this.emojiMapping.put("😲", ":-O");
        this.emojiMapping.put("😲", ":-o");
        this.emojiMapping.put("😲", ":O");
        this.emojiMapping.put("😲", ":o");
        this.emojiMapping.put("😲", "O-O");
        this.emojiMapping.put("😲", "O_O");
        this.emojiMapping.put("😲", "O_o");
        this.emojiMapping.put("😲", "o-o");
        this.emojiMapping.put("😲", "o_O");
        this.emojiMapping.put("😲", "o_o");
        this.emojiMapping.put("😳", ":$");
        this.emojiMapping.put("😵", "#-)");
        this.emojiMapping.put("😶", ":#");
        this.emojiMapping.put("😶", ":&");
        this.emojiMapping.put("😶", ":-#");
        this.emojiMapping.put("😶", ":-&");
        this.emojiMapping.put("😶", ":-X");
        this.emojiMapping.put("😶", ":X");
        this.emojiMapping.put("😼", ":-J");
        this.emojiMapping.put("😽", ":*");
        this.emojiMapping.put("😽", ":^*");
        this.emojiMapping.put("🙅", "ಠ_ಠ");
        this.emojiMapping.put("🙆", "*\\0/*");
        this.emojiMapping.put("🙆", "\\o/");
        this.emojiMapping.put("😄", ":>");
        this.emojiMapping.put("😡", ">.<");
        this.emojiMapping.put("😠", ">:(");
        this.emojiMapping.put("😈", ">:)");
        this.emojiMapping.put("😈", ">:-)");
        this.emojiMapping.put("😡", ">:/");
        this.emojiMapping.put("😲", ">:O");
        this.emojiMapping.put("😜", ">:P");
        this.emojiMapping.put("😒", ">:[");
        this.emojiMapping.put("😡", ">:\\");
        this.emojiMapping.put("😈", ">;)");
        this.emojiMapping.put("😤", ">_>^");
    }



    @Override
    public void onLoad() {
        this.forwardEmojiReplacePattern = compileToPattern(this.emojiMapping.keySet());

        this.emojiMapping.forEach((key, value) -> reverseEmojiMapping.put(value, key));
        this.reverseEmojiReplacePattern = compileToPattern(this.reverseEmojiMapping.keySet());
    }

    private static Pattern compileToPattern(Set<String> options) {
        return Pattern.compile(options.stream().map(Pattern::quote).collect(Collectors.joining("|", "(?:", ")")));
    }
}
