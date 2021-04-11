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

public class DiscordConfig extends ReflectedYaml {

    public String applicationId;
    public String botToken;
    public String channel = "minecraft";
    public String defaultChatFormat = "{NAME} from &5Discord&r: {MESSAGE}";
    public WebhookConfig webhook = new WebhookConfig();

    public static class WebhookConfig implements Section {
        public String id;
        public String token;
    }
}
