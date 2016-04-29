/**
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
package org.cubeengine.module.faq;

import java.util.PriorityQueue;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.text.Text;

import static org.spongepowered.api.event.Order.POST;

@ModuleInfo(name = "Faq", description = "Answers all your frequently asked questions!")
public class Faq extends Module
{
    private final PriorityQueue<Question> questions = new PriorityQueue<>();
    @ModuleConfig private FaqConfig config;
    @Inject private EventManager em;
    @Inject private TaskManager tm;

    @Inject
    public Faq(Reflector reflector)
    {
        reflector.getDefaultConverterManager().registerConverter(new QuestionConverter(), Question.class);
    }

    @Enable
    public void onEnable()
    {
        this.questions.addAll(config.questions);
        em.registerListener(this, this);
    }

    @Listener(order = POST)
    public void onPlayerChat(MessageChannelEvent.Chat event, @First Player player)
    {
        String question = event.getRawMessage().toPlain();
        int questionMarkIndex = question.indexOf('?');
        if (questionMarkIndex > -1)
        {
            question = question.substring(0, questionMarkIndex + 1);
            int dotPosition = question.indexOf('.');
            if (dotPosition > -1 && dotPosition < questionMarkIndex - 2)
            {
                question = question.substring(dotPosition + 1);
            }
            question = question.toLowerCase();

            double score;
            double highestScore = 0;
            Question bestFaq = null;

            for (Question faq : this.questions)
            {
                score = faq.getScore(question);
                if (score == -1 || score > highestScore)
                {
                    highestScore = score;
                    bestFaq = faq;
                    if (highestScore == -1)
                    {
                        break;
                    }
                }
            }

            if (bestFaq != null && (highestScore >= 1.0 || highestScore == -1))
            {
                bestFaq.hit();
                final Text answer = Text.of(bestFaq.getAnswer());
                tm.runTaskDelayed(this, () -> player.sendMessage(answer), 5L);
            }
        }
    }
}
